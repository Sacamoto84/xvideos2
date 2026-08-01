package com.client.xvideos.common.gallery

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.common.snackbar.SnackBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.OutputStream

/**
 * Сохранение медиа в общую галерею, в папку `xvideos_download`.
 *
 * Это единственное место, которое пишет за пределы приложения — и так и
 * задумано: «В галерею» существует ровно для того, чтобы файл был виден
 * системной галерее и пережил удаление приложения. Всё остальное хранится во
 * внутренней памяти (`AppPath`).
 *
 * Запись идёт через `MediaStore`, поэтому разрешения на хранилище не нужны:
 * приложение владеет созданными им записями. Видео уходят в `Movies/`,
 * картинки — в `Pictures/`. `MediaScannerConnection` не нужен, MediaStore
 * индексирует запись сам.
 */
object GallerySaver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Копирует уже скачанный файл в галерею. Fire-and-forget, снекбары внутри. */
    fun saveLocal(context: Context, src: File, fileName: String) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                if (exists(appContext, fileName)) {
                    SnackBar.info("Уже в галерее")
                    return@launch
                }
                publish(appContext, fileName) { output -> src.inputStream().use { it.copyTo(output) } }
                SnackBar.success("Сохранено в галерею")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "GallerySaver: ошибка копирования $fileName")
                SnackBar.error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    /**
     * Качает файл по [url] и кладёт в галерею. Fire-and-forget, снекбары внутри.
     * [progress] — существующий flow зелёного бара раздела
     * (`0..1` — прогресс, `-2` — покой, `-3` — ошибка).
     *
     * Скачивание идёт во временный файл в `cacheDir`: `KDownloader` пишет по
     * пути `File`, а у записи MediaStore пути нет — только поток. После
     * успешной загрузки файл публикуется и временная копия удаляется.
     */
    fun saveFromUrl(
        context: Context,
        kDownloader: KDownloader,
        url: String,
        fileName: String,
        progress: MutableStateFlow<Float>? = null,
    ) {
        val appContext = context.applicationContext

        scope.launch {
            if (exists(appContext, fileName)) {
                SnackBar.info("Уже в галерее")
                return@launch
            }

            val tmpDir = File(appContext.cacheDir, "gallery_tmp").apply { mkdirs() }
            val tmpFile = File(tmpDir, fileName)
            SnackBar.info("Сохранение в галерею…")

            val request = kDownloader.newRequestBuilder(url, tmpDir.absolutePath, fileName).build()
            kDownloader.enqueue(
                request,
                onStart = { progress?.value = 0f },
                onProgress = { p -> progress?.value = p / 100f },
                onCompleted = {
                    scope.launch {
                        try {
                            publish(appContext, fileName) { output ->
                                tmpFile.inputStream().use { it.copyTo(output) }
                            }
                            progress?.value = -2f
                            SnackBar.success("Сохранено в галерею")
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            progress?.value = -3f
                            Timber.e(e, "GallerySaver: ошибка публикации $fileName")
                            SnackBar.error("Ошибка сохранения: ${e.message}")
                        } finally {
                            tmpFile.delete()
                        }
                    }
                },
                onError = { error ->
                    progress?.value = -3f
                    tmpFile.delete()
                    Timber.e("GallerySaver: ошибка скачивания $fileName: $error")
                    SnackBar.error("Ошибка сохранения: $error")
                },
            )
        }
    }

    /**
     * Создаёт запись в MediaStore и отдаёт её поток в [write].
     *
     * Пока идёт запись, запись помечена `IS_PENDING`, поэтому галерея не
     * покажет недокачанный файл. При ошибке запись удаляется, чтобы не
     * оставлять «висящих» пустышек.
     */
    private fun publish(context: Context, fileName: String, write: (OutputStream) -> Unit) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            mimeType(fileName)?.let { put(MediaStore.MediaColumns.MIME_TYPE, it) }
            put(MediaStore.MediaColumns.RELATIVE_PATH, GalleryTarget.relativePath(fileName))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri: Uri = resolver.insert(collectionFor(fileName), values)
            ?: error("MediaStore отказался создать запись для $fileName")

        try {
            resolver.openOutputStream(uri)?.use(write)
                ?: error("Не удалось открыть поток записи для $fileName")
        } catch (e: Throwable) {
            resolver.delete(uri, null, null)
            throw e
        }

        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    }

    /** Есть ли уже такой файл в папке галереи. */
    private fun exists(context: Context, fileName: String): Boolean =
        context.contentResolver.query(
            collectionFor(fileName),
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(GalleryTarget.relativePath(fileName), fileName),
            null
        )?.use { it.count > 0 } ?: false

    private fun collectionFor(fileName: String): Uri =
        if (GalleryTarget.isVideo(fileName)) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

    /** Только для колонки MIME_TYPE — на выбор папки не влияет, см. [GalleryTarget]. */
    private fun mimeType(fileName: String): String? {
        val extension = GalleryTarget.extensionOf(fileName)
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
