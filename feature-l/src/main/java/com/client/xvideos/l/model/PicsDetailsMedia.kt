package com.client.xvideos.l.model

import com.client.xvideos.common.settings.ThumbnailsSize

/**
 * Проверяет, является ли данный элемент анимированным медиа (видео mp4, gif).
 */
fun PicsDetails.isAnimatedMedia(): Boolean {
    if (is_animated) return true
    if (!url_to_video.isNullOrBlank()) return true
    val orig = url_to_original
    if (!orig.isNullOrBlank()) {
        val cleanOrig = orig.substringBefore('?').substringBefore('#')
        if (cleanOrig.endsWith(".gif", ignoreCase = true) || cleanOrig.isLVideoFilePath()) {
            return true
        }
    }
    val thumbs = thumbnails
    if (thumbs.isNullOrEmpty()) return false
    return thumbs.any { thumb ->
        val url = thumb.url ?: return@any false
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        cleanUrl.endsWith(".gif", ignoreCase = true) || cleanUrl.isLVideoFilePath()
    }
}

/**
 * Возвращает URL воспроизводимого видеофайла для анимированного элемента.
 */
fun PicsDetails.lAnimationVideoUrl(): String? {
    if (!isAnimatedMedia()) return null
    return url_to_video?.takeIf { it.isNotBlank() }
        ?: url_to_original?.takeIf { it.isLVideoFileUrl() }
}

/**
 * Безопасно вычисляет соотношение сторон картинки (width / height).
 */
fun PicsDetails.safeAspectRatio(): Float {
    return if (width > 0 && height > 0) width.toFloat() / height else 1f
}

/**
 * Возвращает наилучший URL для скачивания файла (видео или картинки).
 */
fun PicsDetails.lDownloadUrl(): String? {
    return if (isAnimatedMedia()) {
        lAnimationVideoUrl() ?: lImageMediaUrl()
    } else {
        lImageMediaUrl()
    }
}

/**
 * Возвращает URL превью заданного размера [thumbnailsSize].
 */
fun PicsDetails.lPreviewImageUrl(thumbnailsSize: String): String {
    return thumbnails
        ?.firstOrNull { it.size == thumbnailsSize }
        ?.url
        ?.takeIf { it.isNotBlank() }
        ?: thumbnails
            ?.firstOrNull { !it.url.isNullOrBlank() && !it.url.isLVideoFileUrl() }
            ?.url
        ?: lImageMediaUrl().orEmpty()
}

/**
 * Возвращает список URL изображений для полноэкранного просмотра (с приоритетом локального файла).
 */
fun PicsDetails.lFullScreenImageUrls(): List<String> {
    val localOriginal = url_to_original
        ?.takeIf { it.isLocalImagePath() }

    val thumbnails = lThumbnailImageUrlsBySize()

    val fallbackOriginal = url_to_original
        ?.takeIf { thumbnails.isEmpty() && it.isNotBlank() && !it.isLVideoFileUrl() }

    val capacity = thumbnails.size + (if (localOriginal != null) 1 else 0) + (if (fallbackOriginal != null) 1 else 0)
    if (capacity == 0) return emptyList()

    val result = ArrayList<String>(capacity)
    if (localOriginal != null) {
        result.add(localOriginal)
    }
    for (thumb in thumbnails) {
        if (thumb != localOriginal) {
            result.add(thumb)
        }
    }
    if (fallbackOriginal != null && fallbackOriginal != localOriginal) {
        result.add(fallbackOriginal)
    }
    return result
}

/**
 * Возвращает лучший доступный URL статического изображения.
 */
fun PicsDetails.lImageMediaUrl(): String? {
    val localOriginal = url_to_original?.takeIf { it.isLocalImagePath() }
    return localOriginal
        ?: lBestThumbnailImageUrl()
        ?: url_to_original?.takeIf { it.isNotBlank() && !it.isLVideoFileUrl() }
}

/**
 * Возвращает URL наибольшей по площади миниатюры.
 */
fun PicsDetails.lBestThumbnailImageUrl(): String? {
    return lThumbnailImageUrlsBySize().firstOrNull()
}

/**
 * Возвращает список URL всех миниатюр, отсортированных по убыванию разрешения.
 */
fun PicsDetails.lThumbnailImageUrlsBySize(): List<String> {
    val thumbs = thumbnails
    if (thumbs.isNullOrEmpty()) return emptyList()
    return thumbs
        .asSequence()
        .filter {
            val url = it.url
            !url.isNullOrBlank() && !url.isLVideoFileUrl()
        }
        .sortedWith(compareByDescending<Thumbnails> {
            it.width.coerceAtLeast(0).toLong() * it.height.coerceAtLeast(0).toLong()
        }.thenByDescending {
            if (it.size == ThumbnailsSize.XMAX.value) 1 else 0
        })
        .mapNotNull { it.url }
        .distinct()
        .toList()
}

/**
 * Формирует уникальное безопасное имя файла для сохранения на локальный диск.
 */
fun PicsDetails.lSavedFileName(): String? {
    val sourceName = lDownloadUrl()?.lUrlFileName()?.takeIf { it.isNotBlank() } ?: return null
    val cleanSourceName = sourceName.replace('/', '_').replace('\\', '_')
    val rawAlbum = album?.takeIf { it.isNotBlank() } ?: "null"
    val cleanAlbum = rawAlbum
        .replace("..", "_")
        .replace('/', '_')
        .replace('\\', '_')
        .trim()
        .ifEmpty { "null" }
    val candidate = "${width}_${height}_${is_animated}_${cleanAlbum}_$cleanSourceName"
    return candidate.replace("..", "_")
}

/** Заголовки HTTP для запросов медиафайлов Luscious. */
fun lMediaRequestHeaders(): Map<String, String> = L_MEDIA_REQUEST_HEADERS

/** Заголовки загрузки для KDownloader. */
fun lMediaDownloadHeaders(): HashMap<String, List<String>> = HashMap(L_MEDIA_DOWNLOAD_HEADERS)

fun lMediaUserAgent(): String = L_MEDIA_USER_AGENT

internal fun String.isLVideoFilePath(): Boolean =
    endsWith(".mp4", ignoreCase = true) ||
    endsWith(".webm", ignoreCase = true) ||
    endsWith(".m3u8", ignoreCase = true) ||
    endsWith(".m4v", ignoreCase = true) ||
    endsWith(".mov", ignoreCase = true)

fun String.isLVideoFileUrl(): Boolean =
    substringBefore('?').substringBefore('#').isLVideoFilePath()

fun String.lUrlFileName(): String {
    return substringBefore('?').substringBefore('#').substringAfterLast('/')
}

fun String.lUrlExtension(): String {
    return lUrlFileName().substringAfterLast('.', missingDelimiterValue = "")
}

fun String.isLImageFileUrl(): Boolean {
    if (isLVideoFileUrl()) return false
    val ext = lUrlExtension().lowercase()
    return ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp" || ext == "gif"
}

private fun String.isLocalImagePath(): Boolean {
    return isNotBlank() &&
            !startsWith("http://", ignoreCase = true) &&
            !startsWith("https://", ignoreCase = true) &&
            !isLVideoFileUrl()
}

private const val L_MEDIA_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36"

private val L_MEDIA_REQUEST_HEADERS: Map<String, String> = mapOf(
    "User-Agent" to L_MEDIA_USER_AGENT,
    "Referer" to "https://www.luscious.net/",
    "Origin" to "https://www.luscious.net",
    "Accept" to "*/*",
    "Accept-Encoding" to "identity",
    "Accept-Language" to "ru,en;q=0.9"
)

private val L_MEDIA_DOWNLOAD_HEADERS: HashMap<String, List<String>> = HashMap<String, List<String>>().apply {
    L_MEDIA_REQUEST_HEADERS
        .filterKeys { it != "User-Agent" }
        .forEach { (key, value) -> put(key, listOf(value)) }
}

private val ANCHOR_ID_REGEX = Regex("""/id/(\d+)""")

/**
 * Извлекает ID картинки для серверных мутаций (например, FavoriteAdd).
 * Сначала проверяет поле [PicsDetails.id], затем пытается извлечь числовой ID из URL.
 */
fun PicsDetails.extractAnchorId(): String? {
    if (!id.isNullOrBlank()) return id.trim()
    url?.let { ANCHOR_ID_REGEX.find(it)?.groupValues?.getOrNull(1) }?.let { return it }
    url_to_original?.let { ANCHOR_ID_REGEX.find(it)?.groupValues?.getOrNull(1) }?.let { return it }
    url_to_video?.let { ANCHOR_ID_REGEX.find(it)?.groupValues?.getOrNull(1) }?.let { return it }
    thumbnails?.forEach { thumb ->
        thumb.url?.let { ANCHOR_ID_REGEX.find(it)?.groupValues?.getOrNull(1) }?.let { return it }
    }
    return null
}

val PicsDetails.hasAnchorId: Boolean get() = extractAnchorId() != null

fun PicsDetails.extractAnchorIdOrEmpty(): String = extractAnchorId() ?: ""
