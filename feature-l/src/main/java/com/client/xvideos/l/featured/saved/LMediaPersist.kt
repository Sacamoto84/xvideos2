package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.extractAnchorId
import com.client.xvideos.l.model.Thumbnails
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lMediaRequestHeaders
import com.client.xvideos.l.model.lUrlExtension
import com.client.xvideos.l.model.lUrlFileName
import com.client.xvideos.l.net.LAlbumBundleCache
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.LusciousEndpoints
import com.client.xvideos.l.net.L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS
import com.client.xvideos.l.net.L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION
import com.client.xvideos.l.net.graphQl.getAlbumInfo
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.RepositoryUriConfig
import io.ktor.client.HttpClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/** Имя файла метаданных сохраненного элемента внутри его каталога. */
internal const val L_METADATA_FILE_NAME = "metadata.json"

/**
 * Расширение недокачанного файла. Такой файл остаётся, если процесс убили
 * посреди загрузки, и его нельзя показывать как содержимое.
 */
internal const val L_PART_FILE_SUFFIX = ".part"

/** Проверяет, является ли файл временным недокачанным фрагментом с суффиксом `.part`. */
internal fun File.isPartialDownload(): Boolean = name.endsWith(L_PART_FILE_SUFFIX)

/** Стандартный размер буфера потокового чтения и записи (8 КБ). */
private const val DEFAULT_BUFFER_SIZE = 8 * 1024

/**
 * Создаёт настроенный [HttpClient] для скачивания media-файлов luscious с поддержкой DoH и таймаутов.
 */
internal fun lCreateMediaClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            dns(AppDns)
        }
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 60_000
    }
    defaultRequest {
        headers {
            lMediaRequestHeaders().forEach { (key, value) -> append(key, value) }
        }
    }
}

/**
 * Описание одного варианта превью, который мы уже извлекли из [PicsDetails].
 *
 * @property url Сетевой URL превью.
 * @property width Ширина изображения в пикселях.
 * @property height Высота изображения в пикселях.
 * @property size Текстовая метка размера ("small", "medium", "large_thumbnail" и т.д.).
 * @property sizeMarker Суффикс размера для формирования имени файла на диске.
 * @property extension Расширение файла ("jpg", "webp", "png").
 */
internal data class LPreviewSource(
    val url: String,
    val width: Int,
    val height: Int,
    val size: String?,
    val sizeMarker: String,
    val extension: String
) {
    /** Конвертирует источник превью в сохраняемую запись метаданных [LSavedLikePreview]. */
    fun toSavedPreview(fileName: String): LSavedLikePreview = LSavedLikePreview(
        fileName = fileName,
        sourceUrl = url,
        width = width,
        height = height,
        size = size
    )
}

/**
 * Извлекает уникальные источники статических превью из миниатюр [PicsDetails], отсортированные по возрастанию площади.
 */
internal fun PicsDetails.lPreviewSources(): List<LPreviewSource> {
    return thumbnails
        ?.mapNotNull { it.toPreviewSource() }
        ?.distinctBy { it.url.substringBefore('?').substringBefore('#') }
        ?.sortedBy { it.width * it.height }
        ?: emptyList()
}

/**
 * Преобразует [Thumbnails] в [LPreviewSource], отфильтровывая пустые и видео-URL.
 */
private fun Thumbnails.toPreviewSource(): LPreviewSource? {
    val sourceUrl = url?.takeIf { it.isNotBlank() && !it.isLVideoFileUrl() } ?: return null
    return LPreviewSource(
        url = sourceUrl,
        width = width,
        height = height,
        size = size,
        sizeMarker = sourceUrl.previewSizeMarker(width, height),
        extension = sourceUrl.imageExtension()
    )
}

/**
 * Формирует детерминированное имя директории для сохранения элемента на основе альбома, SHA-256 URL и имени файла.
 */
internal fun lBuildFolderName(item: PicsDetails, mediaUrl: String): String {
    val album = item.album?.takeIf { it.isNotBlank() && it != "null" } ?: "no_album"
    val baseName = mediaUrl.lUrlFileName()
        .substringBeforeLast('.', missingDelimiterValue = mediaUrl.lUrlFileName())
        .sanitizeFilePart()
        .take(60)
        .ifBlank { "media" }
    return "${album.sanitizeFilePart()}_${mediaUrl.sha256().take(12)}_$baseName"
}

/** Извлекает нормализованное расширение графического файла (jpg, jpeg, png, webp, gif). */
internal fun String.imageExtension(): String =
    lUrlExtension().lowercase()
        .takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif") }
        ?: "jpg"

/** Извлекает нормализованное расширение видеофайла (mp4, webm, m4v, mov). */
internal fun String.videoExtension(): String =
    lUrlExtension().lowercase()
        .takeIf { it in setOf("mp4", "webm", "m4v", "mov") }
        ?: "mp4"

private val PREVIEW_SIZE_MARKER_REGEX = Regex("\\.(\\d+x\\d+)\\.[^.]+$")
private val SANITIZE_FILE_PART_REGEX = Regex("[^A-Za-z0-9._-]")

/** Извлекает маркер разрешения вида `300x400` из URL или генерирует его из переданных [width] и [height]. */
private fun String.previewSizeMarker(width: Int, height: Int): String {
    return PREVIEW_SIZE_MARKER_REGEX
        .find(lUrlFileName())
        ?.groupValues
        ?.getOrNull(1)
        ?: "${width}x${height}"
}

/** Очищает строку от спецсимволов для безопасного использования в качестве части имени файла/папки. */
internal fun String.sanitizeFilePart(): String =
    replace(SANITIZE_FILE_PART_REGEX, "_").trim('_')

/** Вычисляет SHA-256 хэш строки в hex-формате. */
internal fun String.sha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

/** Удаляет префикс схемы `file://` при наличии. */
internal fun String.lToFilePath(): String = removePrefix("file://")

/** Преобразует строку в существующий локальный [File] либо возвращает `null`, если строка является сетевым URL или файл отсутствует. */
internal fun String.lToLocalFileOrNull(): File? {
    if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) {
        return null
    }
    return File(lToFilePath()).takeIf { it.exists() && it.isFile }
}

/** Проверяет, что [file] физически находится внутри директории [root] (защита от path traversal). */
internal fun lIsInside(root: File, file: File): Boolean = runCatching {
    val rootPath = root.canonicalFile.absolutePath
    val filePath = file.canonicalFile.absolutePath
    filePath == rootPath || filePath.startsWith(rootPath + File.separator)
}.getOrDefault(false)

/* ---------- Скачивание / копирование ---------- */

/**
 * Скачивает файл по [url] в [file] через временный `.part` файл с контролем размера.
 *
 * @param client Экземпляр [HttpClient].
 * @param url Сетевой адрес файла.
 * @param file Целевой локальный файл.
 * @param onProgress Обратный вызов обновления прогресса скачивания в байтах.
 */
internal suspend fun lDownloadToFile(
    client: HttpClient,
    url: String,
    file: File,
    onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
) {
    if (file.exists() && file.length() > 0L) return
    file.parentFile?.mkdirs()

    val tempFile = File(file.parentFile, "${file.name}$L_PART_FILE_SUFFIX")
    try {
        val response: HttpResponse = client.get(url)
        if (!response.status.isSuccess()) {
            throw IOException("HTTP error: ${response.status.value}")
        }
        val totalBytes = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        var downloadedBytes = 0L
        response.bodyAsChannel().toInputStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    downloadedBytes += count
                    onProgress(downloadedBytes, totalBytes)
                }
            }
        }
        if (totalBytes != null && totalBytes > 0L && downloadedBytes < totalBytes) {
            throw IOException("Download incomplete: expected $totalBytes bytes, got $downloadedBytes bytes")
        }
        if (downloadedBytes == 0L) {
            throw IOException("Download failed: empty response body (0 bytes) from $url")
        }
        if (file.exists() && !file.delete()) {
            throw IOException("Cannot replace file: ${file.absolutePath}")
        }
        if (!tempFile.renameTo(file)) {
            tempFile.copyTo(file, overwrite = true)
            tempFile.delete()
        }
    } catch (e: Exception) {
        tempFile.delete()
        throw e
    }
}

/**
 * Скачивает файл по сети с регистрацией прогресса в [progress].
 */
internal suspend fun lDownloadToFileTracked(
    client: HttpClient,
    url: String,
    file: File,
    progress: LDownloadProgress
) {
    val progressId = progress.startFile()
    try {
        lDownloadToFile(client, url, file) { downloaded, total ->
            val fraction = when {
                total != null && total > 0L -> downloaded.toFloat() / total.toFloat()
                downloaded > 0L -> 0.05f
                else -> 0f
            }
            progress.updateFile(progressId, fraction)
        }
    } finally {
        progress.finishFile(progressId)
    }
}

/**
 * Копирует локальный файл [source] в [file] с регистрацией прогресса в [progress].
 */
internal fun lCopyToFileTracked(
    source: File,
    file: File,
    progress: LDownloadProgress
) {
    val progressId = progress.startFile()
    try {
        file.parentFile?.mkdirs()
        source.copyTo(file, overwrite = true)
        progress.updateFile(progressId, 1f)
    } finally {
        progress.finishFile(progressId)
    }
}

/**
 * Сохраняет медиаисточник (локальный файл либо сетевой URL) в файл назначения [file] с отслеживанием прогресса.
 */
internal suspend fun lSaveMediaSourceTracked(
    client: HttpClient,
    source: String,
    file: File,
    progress: LDownloadProgress
) {
    val localFile = source.lToLocalFileOrNull()
    if (localFile != null) {
        lCopyToFileTracked(localFile, file, progress)
    } else {
        lDownloadToFileTracked(client, source, file, progress)
    }
}

/* ---------- Album info ---------- */

/**
 * Получает детальные метаданные альбома Luscious по его [albumId], проверяя кэш бандлов перед обращением в сеть.
 */
internal suspend fun lFetchAlbumDetails(luscious: Luscious, albumId: Int): AlbumDetails? {
    val cachedBundle = luscious.repository.getAlbumBundleCache(
        albumId = albumId,
        maxAgeMs = L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS
    )
    if (cachedBundle != null) {
        val bundle = cachedBundle.parseAlbumBundleCache()
        if (
            bundle != null &&
            bundle.schemaVersion == L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION &&
            bundle.album.id.isNotBlank()
        ) {
            return bundle.album
        }
        luscious.repository.deleteAlbumBundleCache(albumId)
    }

    val query = getAlbumInfo(albumId)
    return luscious.repository.openURI(query, config = RepositoryUriConfig.DIRECT)
        .getOrNull()
        ?.parseAlbumDetails()
}

/** Разбирает JSON бандла кэша альбома. */
private fun String.parseAlbumBundleCache(): LAlbumBundleCache? = runCatching {
    LJson.decodeFromString<LAlbumBundleCache>(this)
}.onFailure {
    Timber.w(it, "L album bundle cache parse failed")
}.getOrNull()

/** Разбирает GraphQL-ответ с полями `data.album.get` в [AlbumDetails]. */
private fun String.parseAlbumDetails(): AlbumDetails? = runCatching {
    val get = LJson.parseToJsonElement(this).jsonObject["data"]
        ?.jsonObject
        ?.get("album")
        ?.jsonObject
        ?.get("get")
        ?: return null
    LJson.decodeFromJsonElement<AlbumDetails>(get)
}.onFailure {
    Timber.w(it, "L album metadata parse failed")
}.getOrNull()

/* ---------- Сохранение PicsDetails в папку ---------- */

/**
 * Скачивает media и превью элемента [item] в новую папку внутри [root] и
 * записывает в неё [L_METADATA_FILE_NAME]. Имя папки строится из id альбома
 * и SHA-256 источника, чтобы повторное сохранение того же [item] попадало в
 * ту же папку. Прогресс агрегируется через [progress].
 *
 * Используется и в likes, и в collection — единственное отличие L_Likes/L_Collection
 * это корневая директория [root].
 *
 * @param item Сохраняемый элемент с изображениями/видео.
 * @param root Корневая папка (`AppPath.l_likes` или папка коллекции).
 * @param luscious Ссылка на сервис API Luscious.
 * @param progress Трекер совокупного прогресса скачивания.
 * @return Результат [Result] с папкой [File] сохранённого элемента.
 */
internal suspend fun lPersistPicsDetailsToFolder(
    item: PicsDetails,
    root: File,
    luscious: Luscious,
    progress: LDownloadProgress
): Result<File> {
    var progressStarted = false
    return runCatching {
        val previewSources = item.lPreviewSources()
        val mediaUrl = item.lDownloadUrl()
            ?: previewSources.maxByOrNull { it.width * it.height }?.url
            ?: error("Missing media url")
        val expectedFileCount = if (item.is_animated) {
            1 + if (previewSources.isNotEmpty()) 1 else 0
        } else {
            1 + previewSources.size
        }
        progress.begin(expectedFileCount)
        progressStarted = true

        val albumId = item.album?.takeIf { it.isNotBlank() && it != "null" }
        val albumDetails = albumId?.toIntOrNull()?.let { lFetchAlbumDetails(luscious, it) }

        root.mkdirs()
        val folder = File(root, lBuildFolderName(item, mediaUrl))
        folder.mkdirs()

        val mediaExtension =
            if (item.is_animated) mediaUrl.videoExtension() else mediaUrl.imageExtension()
        val mediaFile = File(folder, "media.$mediaExtension")
        val savedPreviews = mutableListOf<LSavedLikePreview>()

        val client = lCreateMediaClient()
        try {
            val mediaSaved = if (item.is_animated) {
                lSaveMediaSourceTracked(client, mediaUrl, mediaFile, progress)
                true
            } else {
                runCatchingCancellable { lSaveMediaSourceTracked(client, mediaUrl, mediaFile, progress) }
                    .onFailure { error ->
                        mediaFile.delete()
                        Timber.w(error, "L media original download failed, fallback to previews: $mediaUrl")
                    }
                    .isSuccess
            }

            if (item.is_animated) {
                previewSources.minByOrNull { it.width * it.height }?.let { preview ->
                    val previewFile = File(folder, "preview.${preview.extension}")
                    runCatchingCancellable { lSaveMediaSourceTracked(client, preview.url, previewFile, progress) }
                        .onSuccess { savedPreviews.add(preview.toSavedPreview(previewFile.name)) }
                        .onFailure { Timber.w(it, "L video preview download failed: ${preview.url}") }
                }
            } else {
                previewSources.forEach { preview ->
                    val previewFile = File(folder, "preview.${preview.sizeMarker}.${preview.extension}")
                    runCatchingCancellable { lSaveMediaSourceTracked(client, preview.url, previewFile, progress) }
                        .onSuccess { savedPreviews.add(preview.toSavedPreview(previewFile.name)) }
                        .onFailure { Timber.w(it, "L preview download failed: ${preview.url}") }
                }
            }

            if (!mediaSaved && savedPreviews.isEmpty()) {
                error("Missing downloaded media and previews")
            }

            val pictureId = item.id?.takeIf { it.isNotBlank() } ?: item.extractAnchorId()
            val metadata = LSavedLikeMetadata(
                folderName = folder.name,
                mediaFileName = mediaFile.name,
                previewFileName = savedPreviews.minByOrNull { it.width * it.height }?.fileName,
                previewFiles = savedPreviews,
                sourceMediaUrl = mediaUrl,
                sourcePreviewUrl = savedPreviews.minByOrNull { it.width * it.height }?.sourceUrl,
                sourceOriginalUrl = item.url_to_original,
                sourceVideoUrl = item.url_to_video,
                albumId = albumId,
                albumTitle = albumDetails?.title,
                albumDescription = albumDetails?.description,
                albumUrl = albumDetails?.url?.let { LusciousEndpoints.HOME + it },
                albumDownloadUrl = albumDetails?.download_url?.let { LusciousEndpoints.HOME + it },
                albumDetails = albumDetails,
                pictureId = pictureId,
                pictureUrl = item.url,
                picture = if (item.id.isNullOrBlank() && !pictureId.isNullOrBlank()) item.copy(id = pictureId) else item
            )
            writeLSavedLikeMetadata(File(folder, L_METADATA_FILE_NAME), metadata)

            // Подчищаем недокачанные хвосты: элемент собран, все загрузки в эту
            // папку завершены, значит любой оставшийся .part — мусор от прошлой
            // попытки, а не чужая активная загрузка.
            folder.listFiles()
                ?.filter { it.isFile && it.isPartialDownload() }
                ?.forEach { stale ->
                    Timber.i("L cleanup stale part file: ${stale.name}")
                    stale.delete()
                }
        } catch (e: Exception) {
            if (folder.listFiles().isNullOrEmpty() || !File(folder, L_METADATA_FILE_NAME).exists()) {
                folder.deleteRecursively()
            }
            throw e
        } finally {
            client.close()
        }
        folder
    }.also {
        if (progressStarted) progress.finish()
    }
}
