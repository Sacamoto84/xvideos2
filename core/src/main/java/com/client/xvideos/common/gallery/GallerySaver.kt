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
import kotlinx.coroutines.launch
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

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
        val trimmed = fileName.trim()
        if (trimmed.isEmpty()) {
            Timber.w("GallerySaver: отклонён пустой fileName")
            SnackBar.error("Недопустимое имя файла")
            return
        }
        val cleanFileName = File(trimmed).name.trim()
        if (cleanFileName.isEmpty() || isUnsafeItemName(cleanFileName)) {
            Timber.w("GallerySaver: отклонён небезопасный fileName: $fileName")
            SnackBar.error("Недопустимое имя файла")
            return
        }
        scope.launch {
            try {
                if (!src.exists() || src.length() == 0L) {
                    Timber.w("GallerySaver: исходный файл пустой или отсутствует: ${src.absolutePath}")
                    SnackBar.error("Файл повреждён или отсутствует")
                    return@launch
                }
                if (exists(appContext, cleanFileName)) {
                    SnackBar.info("Уже в галерее")
                    return@launch
                }
                publish(appContext, cleanFileName) { output -> src.inputStream().use { it.copyTo(output) } }
                SnackBar.success("Сохранено в галерею")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "GallerySaver: ошибка копирования $cleanFileName")
                SnackBar.error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    /** Имена файлов, которые сейчас качаются для галереи. */
    private val savingNames = ConcurrentHashMap.newKeySet<String>()

    /**
     * Качает файл по [url] и кладёт в галерею. Fire-and-forget, снекбары внутри.
     *
     * Скачивание идёт во временный файл в `cacheDir`: `KDownloader` пишет по
     * пути `File`, а у записи MediaStore пути нет — только поток. После
     * успешной загрузки файл публикуется и временная копия удаляется.
     *
     * Временный файл назван по [fileName], поэтому тот же файл дважды
     * одновременно не качается: второй вызов отклоняется. Раньше две загрузки
     * писали в один временный файл — в галерее оказывался битый файл или две
     * записи.
     *
     * Прогресс отдаётся колбэками, а не записью в чужой flow: у раздела свой
     * учёт идущих загрузок, и запись мимо него заставляла индикатор скакать.
     * [onFinished] вызывается ровно один раз на любом исходе, в том числе при
     * отказе до начала загрузки.
     *
     * @param onProgress Доля скачанного, `0f..1f`.
     * @param onFinished Конец сохранения; `failed` — закончилось ли оно ошибкой.
     */
    fun saveFromUrl(
        context: Context,
        kDownloader: KDownloader,
        url: String,
        fileName: String,
        onProgress: (fraction: Float) -> Unit = {},
        onFinished: (failed: Boolean) -> Unit = {},
    ) {
        val appContext = context.applicationContext
        val trimmed = fileName.trim()
        if (trimmed.isEmpty()) {
            Timber.w("GallerySaver: отклонён пустой fileName")
            SnackBar.error("Недопустимое имя файла")
            onFinished(true)
            return
        }
        val cleanFileName = File(trimmed).name.trim()
        if (cleanFileName.isEmpty() || isUnsafeItemName(cleanFileName)) {
            Timber.w("GallerySaver: отклонён небезопасный fileName: $fileName")
            SnackBar.error("Недопустимое имя файла")
            onFinished(true)
            return
        }

        if (url.isBlank() || (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true))) {
            Timber.w("GallerySaver: отклонён некорректный url для $cleanFileName, длина ${url.length}")
            SnackBar.error("Недопустимая ссылка")
            onFinished(true)
            return
        }

        if (!savingNames.add(cleanFileName)) {
            SnackBar.info("Уже сохраняется в галерею")
            onFinished(false)
            return
        }
        val finish: (Boolean) -> Unit = { failed ->
            savingNames.remove(cleanFileName)
            onFinished(failed)
        }

        scope.launch {
            try {
                downloadAndPublish(appContext, kDownloader, url, cleanFileName, onProgress, finish)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "GallerySaver: сохранение $cleanFileName не началось")
                SnackBar.error("Ошибка сохранения: ${e.message}")
                finish(true)
            }
        }
    }

    /** Ставит загрузку во временный файл и публикует его по завершении. [finish] — ровно один раз. */
    private fun downloadAndPublish(
        appContext: Context,
        kDownloader: KDownloader,
        url: String,
        cleanFileName: String,
        onProgress: (fraction: Float) -> Unit,
        finish: (failed: Boolean) -> Unit,
    ) {
        if (exists(appContext, cleanFileName)) {
            SnackBar.info("Уже в галерее")
            finish(false)
            return
        }

        val tmpDir = File(appContext.cacheDir, "gallery_tmp").apply { mkdirs() }
        val tmpFile = File(tmpDir, cleanFileName)
        try {
            requireInside(tmpDir, tmpFile)
        } catch (e: Exception) {
            Timber.w(e, "GallerySaver: небезопасный путь tmpFile")
            SnackBar.error("Недопустимый путь файла")
            finish(true)
            return
        }
        SnackBar.info("Сохранение в галерею…")

        val request = kDownloader.newRequestBuilder(url, tmpDir.absolutePath, cleanFileName).build()
        kDownloader.enqueue(
            request,
            onStart = { onProgress(0f) },
            onProgress = { p -> onProgress(p / 100f) },
            onCompleted = {
                scope.launch {
                    val failed = try {
                        if (!tmpFile.exists() || tmpFile.length() == 0L) {
                            error("Скачанный файл пустой или повреждён")
                        }
                        publish(appContext, cleanFileName) { output ->
                            tmpFile.inputStream().use { it.copyTo(output) }
                        }
                        SnackBar.success("Сохранено в галерею")
                        false
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "GallerySaver: ошибка публикации $cleanFileName")
                        SnackBar.error("Ошибка сохранения: ${e.message}")
                        true
                    } finally {
                        tmpFile.delete()
                    }
                    finish(failed)
                }
            },
            onError = { error ->
                tmpFile.delete()
                // При сбое сети текст — toString() исключения: после двоеточия в нём хост.
                Timber.e("GallerySaver: ошибка скачивания $cleanFileName: ${error.substringBefore(':')}")
                SnackBar.error("Ошибка сохранения: $error")
                finish(true)
            },
            onCancelled = {
                tmpFile.delete()
                finish(false)
            },
        )
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
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Throwable) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /** Есть ли уже такой файл в папке галереи. */
    private fun exists(context: Context, fileName: String): Boolean =
        runCatching {
            val relPath = GalleryTarget.relativePath(fileName)
            context.contentResolver.query(
                collectionFor(fileName),
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf(relPath, fileName),
                null
            )?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)

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
