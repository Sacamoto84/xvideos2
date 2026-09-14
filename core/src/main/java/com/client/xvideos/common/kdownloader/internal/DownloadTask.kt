package com.client.xvideos.common.kdownloader.internal

import com.client.xvideos.common.kdownloader.database.AppDbHelper
import com.client.xvideos.common.kdownloader.database.DbHelper
import com.client.xvideos.common.kdownloader.database.DownloadModel
import com.client.xvideos.common.kdownloader.httpclient.DefaultHttpClient
import com.client.xvideos.common.kdownloader.httpclient.HttpClient
import com.client.xvideos.common.kdownloader.internal.stream.FileDownloadOutputStream
import com.client.xvideos.common.kdownloader.internal.stream.FileDownloadRandomAccessFile
import com.client.xvideos.common.kdownloader.Constants
import com.client.xvideos.common.kdownloader.Status
import com.client.xvideos.common.kdownloader.utils.getPath
import com.client.xvideos.common.kdownloader.utils.getRedirectedConnectionIfAny
import com.client.xvideos.common.kdownloader.utils.getTempPath
import com.client.xvideos.common.kdownloader.utils.renameFileName
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

class DownloadTask(
    private val req: DownloadRequest,
    private val dbHelper: DbHelper
) {

    private var responseCode = 0
    private var totalBytes: Long = 0
    private var inputStream: InputStream? = null
    private var outputStream: FileDownloadOutputStream? = null

    private var tempPath: String = ""
    private var httpClient: HttpClient? = null
    private var isResumeSupported = true

    private var lastSyncTime: Long = 0
    private var lastSyncBytes: Long = 0

    private var eTag: String = ""

    companion object {
        private const val TIME_GAP_FOR_SYNC: Long = 2000
        private const val MIN_BYTES_FOR_SYNC: Long = 65536
        private const val BUFFER_SIZE = 1024 * 4
        private val downloadSemaphore = Semaphore(4)
    }

    suspend inline fun run(
        crossinline onStart: () -> Unit = {},
        crossinline onProgress: (value: Int) -> Unit = { _ -> },
        crossinline onError: (error: String) -> Unit = { _ -> },
        crossinline onCompleted: () -> Unit = {},
        crossinline onPause: () -> Unit = {}
    ) = run(object : DownloadRequest.Listener {
        override fun onStart() = onStart()

        override fun onProgress(value: Int) = onProgress(value)

        override fun onError(error: String) = onError(error)

        override fun onCompleted() = onCompleted()

        override fun onPause() = onPause()
    })

    private suspend fun createAndInsertNewModel() {
        withContext(Dispatchers.IO) {
            dbHelper.insert(
                DownloadModel(
                    id = req.downloadId,
                    url = req.url,
                    totalBytes = req.totalBytes,
                    eTag = eTag
                )
            )
        }
    }

    private suspend fun removeNoMoreNeededModelFromDatabase() {
        withContext(Dispatchers.IO) {
            dbHelper.remove(req.downloadId)
        }
    }



    suspend fun run(listener: DownloadRequest.Listener) {
        downloadSemaphore.withPermit {
            withContext(Dispatchers.IO.limitedParallelism(1)) {
                try {
                    tempPath = getTempPath(req.dirPath, req.fileName)
                    var file = File(tempPath)

                    var model = getDownloadModelIfAlreadyPresentInDatabase()

                    if (model == null && file.exists() && dbHelper is AppDbHelper) {
                        if (!deleteTempFile()) {
                            val parent = file.parentFile ?: File(req.dirPath)
                            file = File(parent, "${file.nameWithoutExtension}_2.${file.extension}")
                            tempPath = file.absolutePath
                        }
                    }

                    if (model != null) {
                        if (file.exists()) {
                            req.totalBytes = (model.totalBytes)
                            req.downloadedBytes = (model.downloadedBytes)
                        } else {
                            removeNoMoreNeededModelFromDatabase()
                            req.downloadedBytes = 0
                            req.totalBytes = 0
                            model = null
                        }
                    }

                    // use the url to download the file with HTTP Client
                    val client = DefaultHttpClient().clone()
                    httpClient = client

                    req.status = Status.RUNNING

                    listener.onStart()

                    client.connect(req)

                    var redirectedClient = getRedirectedConnectionIfAny(client, req)
                    httpClient = redirectedClient
                    responseCode = redirectedClient.getResponseCode()
                    eTag = redirectedClient.getResponseHeader(Constants.ETAG)

                    if (checkIfFreshStartRequiredAndStart(model)) {
                        model = null
                        redirectedClient = httpClient ?: redirectedClient
                    }

                    if (!isSuccessful()) {
                        closeAllSafely(null)
                        deleteTempFile()
                        removeNoMoreNeededModelFromDatabase()
                        req.reset()
                        req.status = Status.FAILED
                        listener.onError("Wrong link")
                        return@withContext
                    }

                    setResumeSupportedOrNot()

                    totalBytes = req.totalBytes

                    if (!isResumeSupported) {
                        deleteTempFile()
                        req.downloadedBytes = 0
                    }

                    if (totalBytes == 0L) {
                        totalBytes = redirectedClient.getContentLength()
                        req.totalBytes = (totalBytes)
                    }

                    if (isResumeSupported && model == null) {
                        createAndInsertNewModel()
                    }

                    inputStream = redirectedClient.getInputStream()
                    if (inputStream == null) {
                        closeAllSafely(null)
                        deleteTempFile()
                        removeNoMoreNeededModelFromDatabase()
                        req.reset()
                        req.status = Status.FAILED
                        listener.onError("Failed to obtain input stream")
                        return@withContext
                    }

                    val buff = ByteArray(BUFFER_SIZE)

                    if (!file.exists()) {
                        val parentFile = file.parentFile
                        if (parentFile != null && !parentFile.exists()) {
                            if (parentFile.mkdirs()) {
                                file.createNewFile()
                            }
                        } else {
                            file.createNewFile()
                        }
                    }

                    val outStream = FileDownloadRandomAccessFile.Companion.create(file)
                    this@DownloadTask.outputStream = outStream

                    if (req.status === Status.CANCELLED) {
                        closeAllSafely(outStream)
                        this@DownloadTask.outputStream = null
                        deleteTempFile()
                        removeNoMoreNeededModelFromDatabase()
                        req.reset()
                        listener.onError("Cancelled")
                        return@withContext
                    } else if (req.status === Status.PAUSED) {
                        sync(outStream)
                        listener.onPause()
                        return@withContext
                    }

                    if (isResumeSupported && req.downloadedBytes != 0L) {
                        outStream.seek(req.downloadedBytes)
                    }

                    var lastProgress = -1
                    do {
                        val byteCount = inputStream!!.read(buff, 0, BUFFER_SIZE)
                        if (byteCount == -1) {
                            break
                        }

                        if (req.status === Status.CANCELLED) {
                            closeAllSafely(outStream)
                            this@DownloadTask.outputStream = null
                            deleteTempFile()
                            removeNoMoreNeededModelFromDatabase()
                            req.reset()
                            listener.onError("Cancelled")
                            return@withContext
                        } else if (req.status === Status.PAUSED) {
                            sync(outStream)
                            listener.onPause()
                            return@withContext
                        }

                        if (!isActive || req.job?.isActive == false) {
                            closeAllSafely(outStream)
                            this@DownloadTask.outputStream = null
                            deleteTempFile()
                            removeNoMoreNeededModelFromDatabase()
                            req.reset()
                            req.status = Status.CANCELLED
                            listener.onError("Cancelled")
                            return@withContext
                        }
                        outStream.write(buff, 0, byteCount)
                        req.downloadedBytes = req.downloadedBytes + byteCount
                        syncIfRequired(outStream)

                        var progress = 0
                        if (totalBytes > 0) {
                            progress = ((req.downloadedBytes * 100) / totalBytes).toInt()
                        }
                        if (progress != lastProgress) {
                            lastProgress = progress
                            listener.onProgress(progress)
                        }
                    } while (true)

                    if (!isActive || req.job?.isActive == false || req.status === Status.CANCELLED) {
                        closeAllSafely(outStream)
                        this@DownloadTask.outputStream = null
                        deleteTempFile()
                        removeNoMoreNeededModelFromDatabase()
                        req.reset()
                        req.status = Status.CANCELLED
                        listener.onError("Cancelled")
                        return@withContext
                    } else if (req.status === Status.PAUSED) {
                        sync(outStream)
                        listener.onPause()
                        return@withContext
                    }

                    if (totalBytes > 0 && req.downloadedBytes < totalBytes) {
                        closeAllSafely(outStream)
                        this@DownloadTask.outputStream = null
                        if (!isResumeSupported) {
                            deleteTempFile()
                            removeNoMoreNeededModelFromDatabase()
                            req.reset()
                        }
                        req.status = Status.FAILED
                        listener.onError("Download incomplete: expected $totalBytes bytes, got ${req.downloadedBytes} bytes")
                        return@withContext
                    }

                    val path = getPath(req.dirPath, req.fileName)
                    closeAllSafely(outStream)
                    this@DownloadTask.outputStream = null
                    renameFileName(tempPath, path)
                    removeNoMoreNeededModelFromDatabase()
                    listener.onCompleted()
                    req.status = Status.COMPLETED
                    return@withContext
                } catch (e: CancellationException) {
                    closeAllSafely(this@DownloadTask.outputStream)
                    this@DownloadTask.outputStream = null
                    deleteTempFile()
                    removeNoMoreNeededModelFromDatabase()
                    req.reset()
                    req.status = Status.CANCELLED
                    listener.onError("Cancelled")
                    throw e
                } catch (e: Exception) {
                    closeAllSafely(this@DownloadTask.outputStream)
                    this@DownloadTask.outputStream = null
                    if (!isResumeSupported) {
                        deleteTempFile()
                        removeNoMoreNeededModelFromDatabase()
                        req.reset()
                    }
                    req.status = Status.FAILED
                    listener.onError(e.toString())
                    return@withContext
                } finally {
                    closeAllSafely(this@DownloadTask.outputStream)
                    this@DownloadTask.outputStream = null
                }
            }
        }
    }

    private fun setResumeSupportedOrNot() {
        isResumeSupported = (responseCode == HttpURLConnection.HTTP_PARTIAL)
    }

    private fun deleteTempFile(): Boolean {
        val file = File(tempPath)
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    @Throws(IOException::class)
    private suspend fun checkIfFreshStartRequiredAndStart(model: DownloadModel?): Boolean {
        if (responseCode == Constants.HTTP_RANGE_NOT_SATISFIABLE || isETagChanged(model)) {
            if (model != null) {
                removeNoMoreNeededModelFromDatabase()
            }
            deleteTempFile()
            req.downloadedBytes = 0
            req.totalBytes = 0
            httpClient?.close()
            var client = DefaultHttpClient().clone()
            httpClient = client
            client.connect(req)
            client = getRedirectedConnectionIfAny(client, req)
            httpClient = client
            responseCode = client.getResponseCode()
            eTag = client.getResponseHeader(Constants.ETAG)
            return true
        }
        return false
    }

    private fun isETagChanged(model: DownloadModel?): Boolean {
        return (!(eTag.isEmpty() || model == null || model.eTag.isEmpty())
                && model.eTag != eTag)
    }

    private suspend fun getDownloadModelIfAlreadyPresentInDatabase(): DownloadModel? {
        return withContext(Dispatchers.IO) {
            dbHelper.find(req.downloadId)
        }
    }

    private suspend fun closeAllSafely(outputStream: FileDownloadOutputStream?) {

        try {
            httpClient?.close()
        } catch (e: Exception) {
            Timber.e(e, "KDownloader: httpClient.close() failed")
        } finally {
            httpClient = null
        }

        try {
            // Было inputStream!!: если загрузка сорвалась до открытия потока,
            // это NPE, а он не IOException — то есть пролетал мимо своего же
            // catch и валил уборку целиком.
            inputStream?.close()
        } catch (e: IOException) {
            Timber.e(e, "KDownloader: inputStream.close() failed")
        } finally {
            inputStream = null
        }

        if (outputStream != null) {
            try {
                sync(outputStream)
            } catch (e: Exception) {
                Timber.e(e, "KDownloader: sync(outputStream) failed")
            } finally {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    Timber.e(e, "KDownloader: outputStream.close() failed")
                }
            }
        }
    }

    private suspend fun syncIfRequired(outputStream: FileDownloadOutputStream) {
        val currentBytes: Long = req.downloadedBytes
        val currentTime = System.currentTimeMillis()
        val bytesDelta: Long = currentBytes - lastSyncBytes
        val timeDelta: Long = currentTime - lastSyncTime
        if (bytesDelta > MIN_BYTES_FOR_SYNC && timeDelta > TIME_GAP_FOR_SYNC) {
            sync(outputStream)
            lastSyncBytes = currentBytes
            lastSyncTime = currentTime
        }
    }

    private suspend fun sync(outputStream: FileDownloadOutputStream) {
        var success: Boolean
        try {
            outputStream.flushAndSync()
            success = true
        } catch (e: IOException) {
            success = false
            Timber.e(e, "KDownloader: flushAndSync() failed")
        }
        if (success && isResumeSupported) {
            dbHelper
                .updateProgress(
                    req.downloadId,
                    req.downloadedBytes,
                    System.currentTimeMillis()
                )
        }
    }

    private fun isSuccessful(): Boolean {
        return (responseCode >= HttpURLConnection.HTTP_OK
                && responseCode < HttpURLConnection.HTTP_MULT_CHOICE)
    }
}
