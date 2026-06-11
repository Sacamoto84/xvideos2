package com.client.xvideos.common.gallery

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.common.snackbar.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Сохранение медиа в `/storage/emulated/0/xvideos_download` — папку, видимую
 * системной галерее (без `.nomedia`, в отличие от служебной `xvideos/`).
 * Запись через File API — у приложения есть MANAGE_EXTERNAL_STORAGE.
 * После записи файл отдаётся MediaScanner'у, иначе галерея его не увидит.
 */
object GallerySaver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val root: File
        get() = File(Environment.getExternalStorageDirectory(), "xvideos_download")

    /** Копирует уже скачанный файл в галерею-папку. Fire-and-forget, снекбары внутри. */
    fun saveLocal(context: Context, src: File, fileName: String) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val dst = File(root, fileName)
                if (dst.exists()) {
                    SnackBar.info("Уже в галерее")
                    return@launch
                }
                root.mkdirs()
                src.copyTo(dst)
                scan(appContext, dst)
                SnackBar.success("Сохранено в галерею")
            } catch (e: Exception) {
                Timber.e(e, "GallerySaver: ошибка копирования $fileName")
                SnackBar.error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    /** Качает файл по [url] прямо в галерею-папку. Fire-and-forget, снекбары внутри. */
    fun saveFromUrl(context: Context, kDownloader: KDownloader, url: String, fileName: String) {
        val appContext = context.applicationContext
        val dst = File(root, fileName)
        if (dst.exists()) {
            SnackBar.info("Уже в галерее")
            return
        }
        root.mkdirs()
        SnackBar.info("Сохранение в галерею…")

        val request = kDownloader.newRequestBuilder(url, root.absolutePath, fileName).build()
        kDownloader.enqueue(
            request,
            onCompleted = {
                scan(appContext, dst)
                SnackBar.success("Сохранено в галерею")
            },
            onError = { error ->
                Timber.e("GallerySaver: ошибка скачивания $fileName: $error")
                SnackBar.error("Ошибка сохранения: $error")
            },
        )
    }

    private fun scan(context: Context, file: File) {
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }
}
