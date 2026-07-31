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
    private lateinit var outputStream: FileDownloadOutputStream

    private var tempPath: String = ""
    private lateinit var httpClient: HttpClient
    private var isResumeSupported = true

    private var lastSyncTime: Long = 0
    private var lastSyncBytes: Long = 0

    private var eTag: String = ""

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1) +
            CoroutineExceptionHandler { _, _ ->

            })

    companion object {
        private const val TIME_GAP_FOR_SYNC: Long = 2000
        private const val MIN_BYTES_FOR_SYNC: Long = 65536
        private const val BUFFER_SIZE = 1024 * 4
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

        dbScope.launch {
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
        dbScope.launch {
            dbHelper.remove(req.downloadId)
        }
    }


    //private val downloadDispatcher = Dispatchers.IO.limitedParallelism(8)

    // Семафор на 8 разрешений
    private val downloadSemaphore = Semaphore(4)

    suspend fun run(listener: DownloadRequest.Listener) {
        downloadSemaphore.withPermit {
            withContext(Dispatchers.IO.limitedParallelism(1)) {
                try {
                    tempPath = getTempPath(req.dirPath, req.fileName)
                    var file = File(tempPath)

                    var model = getDownloadModelIfAlreadyPresentInDatabase()

                    if (model == null && file.exists() && dbHelper is AppDbHelper) {
                        if (!deleteTempFile()) {
                            tempPath =
                                tempPath.split(".")[0] + "2." + tempPath.split(".", limit = 2)[1]
                            file = File(tempPath)
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
                    httpClient = DefaultHttpClient().clone()

                    req.status = Status.RUNNING

                    listener.onStart()

                    httpClient.connect(req)

                    eTag = httpClient.getResponseHeader(Constants.ETAG)

                    if (checkIfFreshStartRequiredAndStart(model)) {
                        model = null
                    }

                    httpClient = getRedirectedConnectionIfAny(httpClient, req)
                    responseCode = httpClient.getResponseCode()

                    if (!isSuccessful()) {
                        listener.onError("Wrong link")
                    }

                    setResumeSupportedOrNot()

                    totalBytes = req.totalBytes

                    if (!isResumeSupported) {
                        deleteTempFile()
                        req.downloadedBytes = 0
                    }

                    if (totalBytes == 0L) {
                        totalBytes = httpClient.getContentLength()
                        req.totalBytes = (totalBytes)
                    }

                    if (isResumeSupported && model == null) {
                        createAndInsertNewModel()
                    }

                    inputStream = httpClient.getInputStream()
                    if (inputStream == null) {
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

                    this@DownloadTask.outputStream = FileDownloadRandomAccessFile.Companion.create(file)

                    if (req.status === Status.CANCELLED) {
                        deleteTempFile()
                        req.reset()
                        listener.onError("Cancelled")
                        return@withContext
                    } else if (req.status === Status.PAUSED) {
                        sync(outputStream)
                        listener.onPause()
                        return@withContext
                    }

                    if (isResumeSupported && req.downloadedBytes != 0L) {
                        outputStream.seek(req.downloadedBytes)
                    }

                    do {
                        val byteCount = inputStream!!.read(buff, 0, BUFFER_SIZE)
                        if (byteCount == -1) {
                            break
                        }

                        if (req.status === Status.CANCELLED) {
                            deleteTempFile()
                            req.reset()
                            listener.onError("Cancelled")
                            return@withContext
                        } else if (req.status === Status.PAUSED) {
                            sync(outputStream)
                            listener.onPause()
                            return@withContext
                        }

                        if (!isActive) {
                            deleteTempFile()
                            req.reset()
                            break
                        }
                        if (!req.job.isActive) {
                            deleteTempFile()
                            req.reset()
                            break
                        }
                        outputStream.write(buff, 0, byteCount)
                        req.downloadedBytes = req.downloadedBytes + byteCount
                        withContext(Dispatchers.IO) {
                            syncIfRequired(outputStream)
                        }

                        var progress = 0
                        if (totalBytes > 0) {
                            progress = ((req.downloadedBytes * 100) / totalBytes).toInt()
                        }
                        listener.onProgress(progress)
                    } while (true)

                    if (req.status === Status.CANCELLED) {
                        deleteTempFile()
                        req.reset()
                        listener.onError("Cancelled")
                        return@withContext
                    } else if (req.status === Status.PAUSED) {
                        sync(outputStream)
                        listener.onPause()
                        return@withContext
                    }

                    val path = getPath(req.dirPath, req.fileName)
                    renameFileName(tempPath, path)
                    listener.onCompleted()
                    req.status = Status.COMPLETED
                    return@withContext
                } catch (e: CancellationException) {
                    deleteTempFile()
                    req.reset()
                    req.status = Status.FAILED
                    listener.onError(e.toString())
                    return@withContext
                } catch (e: Exception) {
                    if (!isResumeSupported) {
                        deleteTempFile()
                        req.reset()
                    }
                    req.status = Status.FAILED
                    listener.onError(e.toString())
                    return@withContext
                } finally {
                    closeAllSafely(outputStream)
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

    @Throws(IOException::class, IllegalAccessException::class)
    private suspend fun checkIfFreshStartRequiredAndStart(model: DownloadModel?): Boolean {
        if (responseCode == Constants.HTTP_RANGE_NOT_SATISFIABLE || isETagChanged(model)) {
            if (model != null) {
                removeNoMoreNeededModelFromDatabase()
            }
            deleteTempFile()
            req.downloadedBytes = 0
            req.totalBytes = 0
            httpClient = DefaultHttpClient().clone()
            httpClient.connect(req)
            httpClient = getRedirectedConnectionIfAny(httpClient, req)
            responseCode = httpClient.getResponseCode()
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

    private suspend fun closeAllSafely(outputStream: FileDownloadOutputStream) {

        try {
            httpClient.close()
        } catch (e: Exception) {
            Timber.e(e, "KDownloader: httpClient.close() failed")
        }

        try {
            inputStream!!.close()
        } catch (e: IOException) {
            Timber.e(e, "KDownloader: inputStream.close() failed")
        }

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