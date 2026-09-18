package com.client.xvideos.l.model

import com.client.xvideos.common.settings.ThumbnailsSize

fun PicsDetails.isAnimatedMedia(): Boolean {
    if (is_animated) return true
    if (!url_to_video.isNullOrBlank()) return true
    val orig = url_to_original
    if (!orig.isNullOrBlank()) {
        val cleanOrig = orig.substringBefore('?').substringBefore('#')
        if (cleanOrig.endsWith(".gif", ignoreCase = true) || orig.isLVideoFileUrl()) {
            return true
        }
    }
    return thumbnails?.any { thumb ->
        val url = thumb.url ?: return@any false
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        cleanUrl.endsWith(".gif", ignoreCase = true) || url.isLVideoFileUrl()
    } == true
}

fun PicsDetails.lAnimationVideoUrl(): String? {
    if (!isAnimatedMedia()) return null
    return url_to_video?.takeIf { it.isNotBlank() }
        ?: url_to_original?.takeIf { it.isLVideoFileUrl() }
}

fun PicsDetails.safeAspectRatio(): Float {
    return if (width > 0 && height > 0) width.toFloat() / height else 1f
}

fun PicsDetails.lDownloadUrl(): String? {
    return if (isAnimatedMedia()) {
        lAnimationVideoUrl() ?: lImageMediaUrl()
    } else {
        lImageMediaUrl()
    }
}

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

fun PicsDetails.lFullScreenImageUrls(): List<String> {
    val localOriginal = url_to_original
        ?.takeIf { it.isLocalImagePath() }

    val thumbnails = lThumbnailImageUrlsBySize()

    val fallbackOriginal = url_to_original
        ?.takeIf { thumbnails.isEmpty() && it.isNotBlank() && !it.isLVideoFileUrl() }

    return (listOfNotNull(localOriginal) + thumbnails + listOfNotNull(fallbackOriginal)).distinct()
}

fun PicsDetails.lImageMediaUrl(): String? {
    val localOriginal = url_to_original?.takeIf { it.isLocalImagePath() }
    return localOriginal
        ?: lBestThumbnailImageUrl()
        ?: url_to_original?.takeIf { it.isNotBlank() && !it.isLVideoFileUrl() }
}

fun PicsDetails.lBestThumbnailImageUrl(): String? {
    return lThumbnailImageUrlsBySize().firstOrNull()
}

fun PicsDetails.lThumbnailImageUrlsBySize(): List<String> {
    return thumbnails
        .orEmpty()
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
        .toList()
        .distinct()
}

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

fun lMediaRequestHeaders(): Map<String, String> {
    return mapOf(
        "User-Agent" to L_MEDIA_USER_AGENT,
        "Referer" to "https://www.luscious.net/",
        "Origin" to "https://www.luscious.net",
        "Accept" to "*/*",
        "Accept-Encoding" to "identity",
        "Accept-Language" to "ru,en;q=0.9"
    )
}

fun lMediaDownloadHeaders(): HashMap<String, List<String>> {
    return HashMap<String, List<String>>().apply {
        lMediaRequestHeaders()
            .filterKeys { it != "User-Agent" }
            .forEach { (key, value) -> put(key, listOf(value)) }
    }
}

fun lMediaUserAgent(): String = L_MEDIA_USER_AGENT

fun String.isLVideoFileUrl(): Boolean {
    val path = substringBefore('?').substringBefore('#')
    return path.endsWith(".mp4", ignoreCase = true) ||
            path.endsWith(".webm", ignoreCase = true) ||
            path.endsWith(".m3u8", ignoreCase = true) ||
            path.endsWith(".m4v", ignoreCase = true) ||
            path.endsWith(".mov", ignoreCase = true)
}

fun String.lUrlFileName(): String {
    return substringBefore('?').substringBefore('#').substringAfterLast('/')
}

fun String.lUrlExtension(): String {
    return lUrlFileName().substringAfterLast('.', missingDelimiterValue = "")
}

private fun String.isLocalImagePath(): Boolean {
    return isNotBlank() &&
            !startsWith("http://", ignoreCase = true) &&
            !startsWith("https://", ignoreCase = true) &&
            !isLVideoFileUrl()
}

private const val L_MEDIA_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36"

/**
 * Извлекает ID картинки для серверных мутаций (например, FavoriteAdd).
 * Сначала проверяет поле [PicsDetails.id], затем пытается извлечь числовой ID из URL.
 */
fun PicsDetails.extractAnchorId(): String? {
    if (!id.isNullOrBlank()) return id.trim()
    val allUrls = listOfNotNull(url, url_to_original, url_to_video) + thumbnails.orEmpty().mapNotNull { it.url }
    for (candidate in allUrls) {
        val match = Regex("""/id/(\d+)""").find(candidate)
        if (match != null) {
            return match.groupValues[1]
        }
    }
    return null
}
