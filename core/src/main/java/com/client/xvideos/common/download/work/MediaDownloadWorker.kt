package com.client.xvideos.common.download.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.net.doh.AppDns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Фоновый воркер скачивания медиа-файлов через WorkManager с поддержкой Foreground Service.
 */
class MediaDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager: NotificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationId: Int = abs(id.hashCode())

    private data class FileTargets(
        val dir: File,
        val targetFile: File,
        val tempFile: File,
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urlString = inputData.getString(DownloadWorkRequest.KEY_URL)
            ?: return@withContext Result.failure(workDataOf(DownloadWorkRequest.KEY_OUTPUT_ERROR to "Missing URL"))
        val destDir = inputData.getString(DownloadWorkRequest.KEY_DEST_DIR)
            ?: return@withContext Result.failure(workDataOf(DownloadWorkRequest.KEY_OUTPUT_ERROR to "Missing destination directory"))
        val fileName = inputData.getString(DownloadWorkRequest.KEY_FILE_NAME)
            ?: return@withContext Result.failure(workDataOf(DownloadWorkRequest.KEY_OUTPUT_ERROR to "Missing file name"))
        val title = inputData.getString(DownloadWorkRequest.KEY_TITLE) ?: fileName
        val metaContent = inputData.getString(DownloadWorkRequest.KEY_META_CONTENT)
        val metaFileName = inputData.getString(DownloadWorkRequest.KEY_META_FILE_NAME)
        val headers = DownloadWorkRequest.parseHeaders(inputData.getString(DownloadWorkRequest.KEY_HEADERS))

        Timber.i("MediaDownloadWorker: Старт загрузки $fileName из $urlString")

        val filesResult = prepareFiles(destDir, fileName, metaFileName)
        val targets = filesResult.getOrElse { err ->
            Timber.e(err)
            return@withContext Result.failure(workDataOf(DownloadWorkRequest.KEY_OUTPUT_ERROR to (err.message ?: "File error")))
        }

        runCatching {
            setForeground(createForegroundInfo(progress = 0, title = title))
        }.onFailure {
            Timber.w(it, "MediaDownloadWorker: Не удалось запустить foreground info (продолжаем в фоне)")
        }

        return@withContext try {
            downloadFile(
                urlString = urlString,
                headers = headers,
                tempFile = targets.tempFile,
                title = title,
            )

            if (isStopped) {
                Timber.w("MediaDownloadWorker: Загрузка отменена или остановлена ОС")
                return@withContext Result.failure()
            }

            finalizeDownloadedFile(targets, metaContent, metaFileName)

            showCompletedNotification(title)
            Timber.i("MediaDownloadWorker: Успешно скачан файл ${targets.targetFile.absolutePath}")

            Result.success(
                workDataOf(
                    DownloadWorkRequest.KEY_OUTPUT_FILE_PATH to targets.targetFile.absolutePath,
                    DownloadWorkRequest.KEY_PROGRESS to 100
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "MediaDownloadWorker: Ошибка при скачивании $fileName")
            showFailedNotification(title, e.message ?: "Ошибка сети")
            Result.failure(
                workDataOf(
                    DownloadWorkRequest.KEY_OUTPUT_ERROR to (e.message ?: "Unknown error")
                )
            )
        }
    }

    private fun prepareFiles(
        destDir: String,
        fileName: String,
        metaFileName: String?
    ): kotlin.Result<FileTargets> {
        if (isUnsafeItemName(fileName)) {
            return kotlin.Result.failure(IllegalArgumentException("Небезопасное имя файла: $fileName"))
        }
        if (metaFileName != null && isUnsafeItemName(metaFileName)) {
            return kotlin.Result.failure(IllegalArgumentException("Небезопасное имя файла метаданных: $metaFileName"))
        }

        val dir = File(destDir)
        if (!dir.exists() && !dir.mkdirs()) {
            return kotlin.Result.failure(IOException("Не удалось создать каталог: ${dir.absolutePath}"))
        }

        val targetFile = File(dir, fileName)
        val tempFile = File(dir, "$fileName.tmp")

        return runCatching {
            requireInside(dir, targetFile)
            requireInside(dir, tempFile)
            FileTargets(dir, targetFile, tempFile)
        }
    }

    private fun finalizeDownloadedFile(
        targets: FileTargets,
        metaContent: String?,
        metaFileName: String?
    ) {
        if (!targets.tempFile.exists()) {
            throw IOException("Временный файл отсутствует после загрузки")
        }

        if (targets.targetFile.exists()) {
            targets.targetFile.delete()
        }

        if (!targets.tempFile.renameTo(targets.targetFile)) {
            targets.tempFile.copyTo(targets.targetFile, overwrite = true)
            targets.tempFile.delete()
        }

        if (!metaContent.isNullOrBlank()) {
            val infoName = metaFileName ?: "${targets.targetFile.nameWithoutExtension}.info"
            File(targets.dir, infoName).writeTextAtomically(metaContent)
        }
    }


    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = inputData.getString(DownloadWorkRequest.KEY_TITLE) ?: "Загрузка"
        return createForegroundInfo(progress = 0, title = title)
    }

    private fun createForegroundInfo(progress: Int, title: String): ForegroundInfo {
        ensureNotificationChannel()
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        val progressText = if (progress >= 0) "$progress%" else "Загрузка..."
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress.coerceIn(0, 100), progress < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отмена", cancelIntent)
            .build()

        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private suspend fun downloadFile(
        urlString: String,
        headers: Map<String, String>,
        tempFile: File,
        title: String
    ) {
        var resumeOffset = if (tempFile.exists()) tempFile.length() else 0L
        var request = buildDownloadRequest(urlString, headers, resumeOffset)

        var call = downloadOkHttpClient.newCall(request)
        var cancellationHandle = kotlin.coroutines.coroutineContext.job.invokeOnCompletion {
            call.cancel()
        }

        try {
            var response = call.execute()

            // Если сервер вернул HTTP 416 (Range Not Satisfiable), значит существующий .tmp
            // повреждён или его размер больше/равен длине файла. Удаляем .tmp и качаем с нуля.
            if (response.code == 416 && resumeOffset > 0) {
                response.close()
                cancellationHandle.dispose()
                Timber.w("MediaDownloadWorker: HTTP 416 для $urlString, удаляем невалидный $tempFile и качаем заново")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                resumeOffset = 0L
                request = buildDownloadRequest(urlString, headers, resumeOffset = 0L)
                call = downloadOkHttpClient.newCall(request)
                cancellationHandle = kotlin.coroutines.coroutineContext.job.invokeOnCompletion {
                    call.cancel()
                }
                response = call.execute()
            }

            val responseCode = response.code
            val isResume = responseCode == 206
            val isOk = response.isSuccessful && !isResume

            if (!isOk && !isResume) {
                val message = response.message
                response.close()
                throw IOException("Сервер вернул HTTP $responseCode: $message")
            }

            val responseBody = response.body
                ?: run {
                    response.close()
                    throw IOException("Сервер вернул пустой ответ без тела (HTTP $responseCode)")
                }
            val totalBytes = calculateTotalBytes(isResume, resumeOffset, responseBody.contentLength())

            val append = isResume
            if (!append && tempFile.exists()) {
                tempFile.delete()
            }

            response.use {
                responseBody.byteStream().use { input ->
                    FileOutputStream(tempFile, append).use { output ->
                        copyStreamWithProgress(
                            input = input,
                            output = output,
                            initialDownloaded = if (append) resumeOffset else 0L,
                            totalBytes = totalBytes,
                            title = title
                        )
                    }
                }
            }
        } finally {
            cancellationHandle.dispose()
        }
    }

    private fun buildDownloadRequest(
        urlString: String,
        headers: Map<String, String>,
        resumeOffset: Long
    ): Request {
        val requestBuilder = Request.Builder().url(urlString)
        val userAgent = headers["User-Agent"] ?: DEFAULT_USER_AGENT
        requestBuilder.header("User-Agent", userAgent)
        headers.forEach { (k, v) ->
            if (!k.equals("User-Agent", ignoreCase = true)) {
                requestBuilder.header(k, v)
            }
        }
        if (resumeOffset > 0) {
            requestBuilder.header("Range", "bytes=$resumeOffset-")
        }
        return requestBuilder.build()
    }

    private fun calculateTotalBytes(isResume: Boolean, resumeOffset: Long, contentLen: Long): Long {
        return if (isResume) {
            if (contentLen > 0) resumeOffset + contentLen else -1L
        } else {
            if (contentLen > 0) contentLen else -1L
        }
    }

    private suspend fun copyStreamWithProgress(
        input: InputStream,
        output: OutputStream,
        initialDownloaded: Long,
        totalBytes: Long,
        title: String
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        var downloadedSoFar = initialDownloaded
        var lastProgressUpdateTime = 0L

        while (input.read(buffer).also { bytesRead = it } != -1) {
            if (isStopped) return

            output.write(buffer, 0, bytesRead)
            downloadedSoFar += bytesRead

            val now = System.currentTimeMillis()
            if (now - lastProgressUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS) {
                lastProgressUpdateTime = now
                updateDownloadProgress(downloadedSoFar, totalBytes, title)
            }
        }
        output.flush()

        if (!isStopped && totalBytes > 0 && downloadedSoFar < totalBytes) {
            throw IOException("Download interrupted: expected $totalBytes bytes, but received only $downloadedSoFar bytes")
        }
    }

    private suspend fun updateDownloadProgress(
        downloadedSoFar: Long,
        totalBytes: Long,
        title: String
    ) {
        val percent = if (totalBytes > 0) {
            ((downloadedSoFar * 100L) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            -1
        }

        setProgress(
            workDataOf(
                DownloadWorkRequest.KEY_PROGRESS to percent.coerceAtLeast(0),
                DownloadWorkRequest.KEY_BYTES_DOWNLOADED to downloadedSoFar,
                DownloadWorkRequest.KEY_TOTAL_BYTES to totalBytes
            )
        )

        runCatching {
            setForeground(createForegroundInfo(progress = percent, title = title))
        }
    }

    private fun ensureNotificationChannel() {
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о фоновой загрузке медиа"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showCompletedNotification(title: String) {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Загрузка завершена")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        runCatching {
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun showFailedNotification(title: String, error: String) {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Ошибка загрузки")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        runCatching {
            notificationManager.notify(notificationId, notification)
        }
    }

    companion object {
        const val WORK_TAG_DOWNLOAD = "media_download"
        const val CHANNEL_ID = "channel_media_downloads"
        const val CHANNEL_NAME = "Загрузки медиа"

        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

        private val downloadOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .dns(AppDns)
                .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }
}
