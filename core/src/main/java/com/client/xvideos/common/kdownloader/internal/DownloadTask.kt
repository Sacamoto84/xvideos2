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

/**
 * Исполнитель отдельной задачи скачивания файла по протоколу HTTP.
 *
 * Отвечает за:
 * - Ограничение параллельных сетевых потоков через [downloadSemaphore] (не более 4 одновременно).
 * - Управление жизненным циклом HTTP-соединения, заголовками Range и ETag.
 * - Обработку редиректов и определение возможности докачки (HTTP 206 Partial Content).
 * - Сброс кэша и перезапуск при смене ETag на сервере или коде 416 (Range Not Satisfiable).
 * - Потоковое чтение данных чанками по [BUFFER_SIZE] (4 КБ).
 * - Периодическую синхронизацию буферов на диск ([flushAndSync]) и сохранение прогресса в SQLite БД.
 * - Защиту от поврежденных файлов: проверка на 0 байт, сверка с Content-Length, атомарное переименование `.temp` файла.
 * - Безопасную очистку ресурсов и дескрипторов при отмене или ошибках.
 *
 * @param req Модель параметров запроса.
 * @param dbHelper Интерфейс базы данных для сохранения прогресса.
 */
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
        /** Минимальный временной интервал между синхронизациями с БД (2 секунды). */
        private const val TIME_GAP_FOR_SYNC: Long = 2000

        /** Минимальный объем скачанных данных между синхронизациями с БД (64 КБ). */
        private const val MIN_BYTES_FOR_SYNC: Long = 65536

        /** Размер промежуточного буфера чтения из сетевого сокета (4 КБ). */
        private const val BUFFER_SIZE = 1024 * 4

        /** Семафор для ограничения одновременных сетевых загрузок до 4 потоков. */
        private val downloadSemaphore = Semaphore(4)
    }

    /**
     * Запуск выполнения задачи с использованием лямбда-коллбэков.
     */
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

    /**
     * Создает новую запись о загрузке в БД на фоновом потоке IO.
     */
    private suspend fun createAndInsertNewModel() {
        withContext(Dispatchers.IO) {
            dbHelper.insert(
                DownloadModel(
                    id = req.downloadId,
                    url = req.url,
                    eTag = eTag,
                    dirPath = req.dirPath,
                    fileName = req.fileName,
                    totalBytes = req.totalBytes,
                    downloadedBytes = req.downloadedBytes,
                    lastModifiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Удаляет запись из БД после завершения или при отмене задачи.
     */
    private suspend fun removeNoMoreNeededModelFromDatabase() {
        withContext(Dispatchers.IO) {
            dbHelper.remove(req.downloadId)
        }
    }

    /**
     * Основной рабочий цикл выполнения задачи скачивания.
     *
     * @param listener Слушатель событий жизненного цикла загрузки.
     */
    suspend fun run(listener: DownloadRequest.Listener) {
        downloadSemaphore.withPermit {
            withContext(Dispatchers.IO.limitedParallelism(1)) {
                var cancelHandler: kotlinx.coroutines.DisposableHandle? = null
                try {
                    tempPath = getTempPath(req.dirPath, req.fileName)
                    var file = File(tempPath)

                    var model = getDownloadModelIfAlreadyPresentInDatabase()

                    // Разрешение коллизии: если на диске остался .temp файл от предыдущего сбоя без записи в БД
                    if (model == null && file.exists() && dbHelper is AppDbHelper) {
                        if (!deleteTempFile()) {
                            val parent = file.parentFile ?: File(req.dirPath)
                            val baseName = file.nameWithoutExtension
                            val ext = file.extension
                            var resolvedFile: File? = null
                            for (counter in 2..100) {
                                val candidate = File(parent, "${baseName}_$counter.$ext")
                                if (!candidate.exists() || candidate.delete()) {
                                    resolvedFile = candidate
                                    break
                                }
                            }
                            val safeFile = resolvedFile
                                ?: File(parent, "${baseName}_${System.currentTimeMillis()}.$ext")
                            file = safeFile
                            tempPath = safeFile.absolutePath
                        }
                    }

                    // Восстановление прогресса из БД при наличии существующего файла
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

                    // Инициализация HTTP-клиента и регистрация хука отмены
                    val client = DefaultHttpClient().clone()
                    httpClient = client
                    cancelHandler = req.job?.invokeOnCompletion {
                        runCatching { httpClient?.close() }
                    }

                    if (req.status == Status.CANCELLED || !isActive || req.job?.isCancelled == true) {
                        req.status = Status.CANCELLED
                        return@withContext
                    }

                    if (req.status == Status.PAUSED) {
                        listener.onPause()
                        return@withContext
                    }

                    req.status = Status.RUNNING
                    listener.onStart()

                    client.connect(req)

                    var redirectedClient = getRedirectedConnectionIfAny(client, req) {
                        httpClient = it
                    }
                    httpClient = redirectedClient
                    responseCode = redirectedClient.getResponseCode()
                    eTag = redirectedClient.getResponseHeader(Constants.ETAG)

                    // Проверка на устаревание ETag или ошибку 416 (сброс к полной перекачке)
                    if (checkIfFreshStartRequiredAndStart(model)) {
                        model = null
                        redirectedClient = httpClient ?: redirectedClient
                    }

                    if (req.status == Status.PAUSED) {
                        closeAllSafely(null)
                        listener.onPause()
                        return@withContext
                    }

                    if (!isSuccessful()) {
                        closeAllSafely(null)
                        deleteTempFile()
                        removeNoMoreNeededModelFromDatabase()
                        val wasCancelled = req.status == Status.CANCELLED || !isActive || req.job?.isCancelled == true
                        req.reset()
                        if (wasCancelled) {
                            req.status = Status.CANCELLED
                            listener.onError("Cancelled")
                        } else {
                            req.status = Status.FAILED
                            listener.onError("Wrong link")
                        }
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
                        val wasCancelled = req.status == Status.CANCELLED || !isActive || req.job?.isCancelled == true
                        req.reset()
                        if (wasCancelled) {
                            req.status = Status.CANCELLED
                            listener.onError("Cancelled")
                        } else {
                            req.status = Status.FAILED
                            listener.onError("Failed to obtain input stream")
                        }
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

                    // Позиционирование указателя записи при докачке
                    if (isResumeSupported && req.downloadedBytes != 0L) {
                        outStream.seek(req.downloadedBytes)
                    }

                    val stream = inputStream ?: run {
                        closeAllSafely(outStream)
                        this@DownloadTask.outputStream = null
                        listener.onError("Input stream closed")
                        return@withContext
                    }

                    var lastProgress = -1
                    // Основной цикл чтения байтов из сети и записи на диск
                    do {
                        val byteCount = stream.read(buff, 0, BUFFER_SIZE)
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
                            progress = ((req.downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
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

                    // Guard 1: Защита от создания 0-байтовых файлов при пустом ответе сервера
                    if (req.downloadedBytes == 0L) {
                        closeAllSafely(outStream)
                        this@DownloadTask.outputStream = null
                        deleteTempFile()
                        removeNoMoreNeededModelFromDatabase()
                        req.reset()
                        req.status = Status.FAILED
                        listener.onError("Download failed: empty response body (0 bytes)")
                        return@withContext
                    }

                    // Guard 2: Защита от недокачанных файлов (сверка с Content-Length)
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

                    // Финальное переименование временного .temp файла в целевой
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
                    val wasCancelled = req.status == Status.CANCELLED || !isActive || req.job?.isCancelled == true
                    val wasPaused = req.status == Status.PAUSED
                    if (wasPaused) {
                        listener.onPause()
                        return@withContext
                    }
                    if (wasCancelled || !isResumeSupported) {
                        deleteTempFile()
                        removeNoMoreNeededModelFromDatabase()
                        req.reset()
                    }
                    if (wasCancelled) {
                        req.status = Status.CANCELLED
                        listener.onError("Cancelled")
                    } else {
                        req.status = Status.FAILED
                        listener.onError(e.toString())
                    }
                    return@withContext
                } finally {
                    cancelHandler?.dispose()
                    closeAllSafely(this@DownloadTask.outputStream)
                    this@DownloadTask.outputStream = null
                }
            }
        }
    }

    /**
     * Сервер поддерживает докачку только при ответе кодом 206 (Partial Content).
     */
    private fun setResumeSupportedOrNot() {
        isResumeSupported = (responseCode == HttpURLConnection.HTTP_PARTIAL)
    }

    /**
     * Удаляет временный файл загрузки с диска.
     */
    private fun deleteTempFile(): Boolean {
        val file = File(tempPath)
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    /**
     * Проверяет, требуется ли полный сброс и перезапуск загрузки с нуля
     * (изменился ETag на сервере или получен HTTP 416 Range Not Satisfiable).
     */
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
            client = getRedirectedConnectionIfAny(client, req) {
                httpClient = it
            }
            httpClient = client
            responseCode = client.getResponseCode()
            eTag = client.getResponseHeader(Constants.ETAG)
            return true
        }
        return false
    }

    /**
     * Сравнивает текущий ETag ответа сервера с сохраненным в БД.
     */
    private fun isETagChanged(model: DownloadModel?): Boolean {
        return (!(eTag.isEmpty() || model == null || model.eTag.isEmpty())
                && model.eTag != eTag)
    }

    /**
     * Извлекает существующую запись загрузки из БД.
     */
    private suspend fun getDownloadModelIfAlreadyPresentInDatabase(): DownloadModel? {
        return withContext(Dispatchers.IO) {
            dbHelper.find(req.downloadId)
        }
    }

    /**
     * Потокобезопасное закрытие всех активных сетевых соединений и файловых потоков с подавлением ошибок.
     */
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

    /**
     * Периодическая проверка необходимости сброса буферов на диск (каждые 64 КБ и 2 секунды).
     */
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

    /**
     * Принудительная синхронизация буферов с физическим носителем и обновление позиции в БД.
     */
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

    /**
     * Проверяет, успешен ли HTTP-код ответа сервера (2xx).
     */
    private fun isSuccessful(): Boolean {
        return (responseCode >= HttpURLConnection.HTTP_OK
                && responseCode < HttpURLConnection.HTTP_MULT_CHOICE)
    }
}
