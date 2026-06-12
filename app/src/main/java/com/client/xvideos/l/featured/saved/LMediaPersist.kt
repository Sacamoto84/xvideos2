package com.client.xvideos.l.featured.saved

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.Thumbnails
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lMediaRequestHeaders
import com.client.xvideos.l.model.lUrlExtension
import com.client.xvideos.l.model.lUrlFileName
import com.client.xvideos.l.net.LAlbumBundleCache
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.net.L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS
import com.client.xvideos.l.net.L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION
import com.client.xvideos.l.net.graphQl.getAlbumInfo
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.ktor.client.HttpClient
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

internal const val L_METADATA_FILE_NAME = "metadata.json"
private const val DEFAULT_BUFFER_SIZE = 8 * 1024

/**
 * Создаёт настроенный [HttpClient] для скачивания media-файлов luscious.
 */
internal fun lCreateMediaClient(): HttpClient = HttpClient(OkHttp) {
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
 */
internal data class LPreviewSource(
    val url: String,
    val width: Int,
    val height: Int,
    val size: String?,
    val sizeMarker: String,
    val extension: String
) {
    fun toSavedPreview(fileName: String): LSavedLikePreview = LSavedLikePreview(
        fileName = fileName,
        sourceUrl = url,
        width = width,
        height = height,
        size = size
    )
}

internal fun PicsDetails.lPreviewSources(): List<LPreviewSource> {
    return thumbnails
        ?.mapNotNull { it.toPreviewSource() }
        ?.distinctBy { it.url.substringBefore('?').substringBefore('#') }
        ?.sortedBy { it.width * it.height }
        ?: emptyList()
}

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

internal fun lBuildFolderName(item: PicsDetails, mediaUrl: String): String {
    val album = item.album?.takeIf { it.isNotBlank() && it != "null" } ?: "no_album"
    val baseName = mediaUrl.lUrlFileName()
        .substringBeforeLast('.', missingDelimiterValue = mediaUrl.lUrlFileName())
        .sanitizeFilePart()
        .take(60)
        .ifBlank { "media" }
    return "${album.sanitizeFilePart()}_${mediaUrl.sha256().take(12)}_$baseName"
}

internal fun String.imageExtension(): String =
    lUrlExtension().lowercase()
        .takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif") }
        ?: "jpg"

internal fun String.videoExtension(): String =
    lUrlExtension().lowercase()
        .takeIf { it in setOf("mp4", "webm", "m4v", "mov") }
        ?: "mp4"

private fun String.previewSizeMarker(width: Int, height: Int): String {
    return Regex("\\.(\\d+x\\d+)\\.[^.]+$")
        .find(lUrlFileName())
        ?.groupValues
        ?.getOrNull(1)
        ?: "${width}x${height}"
}

internal fun String.sanitizeFilePart(): String =
    replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_')

internal fun String.sha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

internal fun String.lToFilePath(): String = removePrefix("file://")

internal fun String.lToLocalFileOrNull(): File? {
    if (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) {
        return null
    }
    return File(lToFilePath()).takeIf { it.exists() && it.isFile }
}

internal fun lIsInside(root: File, file: File): Boolean = runCatching {
    val rootPath = root.canonicalFile.absolutePath
    val filePath = file.canonicalFile.absolutePath
    filePath == rootPath || filePath.startsWith(rootPath + File.separator)
}.getOrDefault(false)

/* ---------- Скачивание / копирование ---------- */

internal suspend fun lDownloadToFile(
    client: HttpClient,
    url: String,
    file: File,
    onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> }
) {
    if (file.exists() && file.length() > 0L) return
    file.parentFile?.mkdirs()

    val tempFile = File(file.parentFile, "${file.name}.part")
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

private fun String.parseAlbumBundleCache(): LAlbumBundleCache? = runCatching {
    Gson().fromJson(this, LAlbumBundleCache::class.java)
}.onFailure {
    Timber.w(it, "L album bundle cache parse failed")
}.getOrNull()

private fun String.parseAlbumDetails(): AlbumDetails? = runCatching {
    val get = JsonParser.parseString(this).asJsonObject["data"]
        ?.asJsonObject
        ?.get("album")
        ?.asJsonObject
        ?.get("get")
        ?.asJsonObject
        ?: return null
    Gson().fromJson(get, AlbumDetails::class.java)
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
 * @return папка сохранённого item'а.
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
                runCatching { lSaveMediaSourceTracked(client, mediaUrl, mediaFile, progress) }
                    .onFailure { error ->
                        mediaFile.delete()
                        Timber.w(error, "L media original download failed, fallback to previews: $mediaUrl")
                    }
                    .isSuccess
            }

            if (item.is_animated) {
                previewSources.minByOrNull { it.width * it.height }?.let { preview ->
                    val previewFile = File(folder, "preview.${preview.extension}")
                    runCatching { lSaveMediaSourceTracked(client, preview.url, previewFile, progress) }
                        .onSuccess { savedPreviews.add(preview.toSavedPreview(previewFile.name)) }
                        .onFailure { Timber.w(it, "L video preview download failed: ${preview.url}") }
                }
            } else {
                previewSources.forEach { preview ->
                    val previewFile = File(folder, "preview.${preview.sizeMarker}.${preview.extension}")
                    runCatching { lSaveMediaSourceTracked(client, preview.url, previewFile, progress) }
                        .onSuccess { savedPreviews.add(preview.toSavedPreview(previewFile.name)) }
                        .onFailure { Timber.w(it, "L preview download failed: ${preview.url}") }
                }
            }

            if (!mediaSaved && savedPreviews.isEmpty()) {
                error("Missing downloaded media and previews")
            }

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
                albumUrl = albumDetails?.url?.let { Luscious.HOME + it },
                albumDownloadUrl = albumDetails?.download_url?.let { Luscious.HOME + it },
                albumDetails = albumDetails,
                picture = item
            )
            writeLSavedLikeMetadata(File(folder, L_METADATA_FILE_NAME), metadata)
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
