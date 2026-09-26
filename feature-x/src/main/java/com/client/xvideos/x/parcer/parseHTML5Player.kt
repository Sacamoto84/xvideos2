package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.normalizeXUrl
import java.util.regex.Pattern

private val PATTERN_VIDEO_TITLE = Pattern.compile("html5player\\.setVideoTitle\\('(.*?)'\\)")
private val PATTERN_ENCODED_ID = Pattern.compile("html5player\\.setEncodedIdVideo\\('(.*?)'\\)")
private val PATTERN_URL_LOW = Pattern.compile("html5player\\.setVideoUrlLow\\('(.*?)'\\)")
private val PATTERN_URL_HIGH = Pattern.compile("html5player\\.setVideoUrlHigh\\('(.*?)'\\)")
private val PATTERN_URL_HLS = Pattern.compile("html5player\\.setVideoHLS\\('(.*?)'\\)")
private val PATTERN_THUMB_URL = Pattern.compile("html5player\\.setThumbUrl\\('(.*?)'\\)")
private val PATTERN_THUMB_URL_169 = Pattern.compile("html5player\\.setThumbUrl169\\('(.*?)'\\)")
private val PATTERN_THUMB_SLIDE = Pattern.compile("html5player\\.setThumbSlide\\('(.*?)'\\)")
private val PATTERN_THUMB_SLIDE_BIG = Pattern.compile("html5player\\.setThumbSlideBig\\('(.*?)'\\)")
private val PATTERN_THUMB_SLIDE_MINUTE = Pattern.compile("html5player\\.setThumbSlideMinute\\('(.*?)'\\)")
private val PATTERN_ID_CDN = Pattern.compile("html5player\\.setIdCDN\\('(.*?)'\\)")
private val PATTERN_ID_CDN_HLS = Pattern.compile("html5player\\.setIdCdnHLS\\('(.*?)'\\)")
private val PATTERN_SEEK_BAR_COLOR = Pattern.compile("html5player\\.setSeekBarColor\\('(.*?)'\\)")
private val PATTERN_UPLOADER_NAME = Pattern.compile("html5player\\.setUploaderName\\('(.*?)'\\)")
private val PATTERN_VIDEO_URL = Pattern.compile("html5player\\.setVideoURL\\('(.*?)'\\)")
private val PATTERN_STATIC_PATH = Pattern.compile("html5player\\.setStaticPath\\('(.*?)'\\)")
private val PATTERN_VIEW_DATA = Pattern.compile("html5player\\.setViewData\\('(.*?)'\\)")

/**
 * Разбирает содержимое скрипта инициализации HTML5-видеоплеера страницы X в объект [HTML5PlayerConfig].
 *
 * Извлекает вызовы `html5player.set*` с помощью регулярных выражений:
 * - URL потоков: High/Low MP4, адаптивный HLS.
 * - URL постеров и спрайтов раскадровки (с декодированием экранированных слешей `\/`).
 * - Метаданные (название, автор, ID ролика).
 *
 * Если ни одного источника воспроизведения не найдено, возвращает `null` (сигнал того, что воспроизведение невозможно).
 *
 * @param script Тело JavaScript блока `<script>` со страницы ролика.
 * @return Распарсенная конфигурация [HTML5PlayerConfig] либо `null`.
 */
fun parseHTML5Player(script: String): HTML5PlayerConfig? {
    val trimmed = script.trim()
    if (trimmed.isEmpty() || !trimmed.contains("html5player")) return null

    val videoUrlLow = extractValue(trimmed, PATTERN_URL_LOW)
    val videoUrlHigh = extractValue(trimmed, PATTERN_URL_HIGH)
    val videoHLS = extractValue(trimmed, PATTERN_URL_HLS)

    val hasAnySource = !videoUrlLow.isNullOrBlank() || !videoUrlHigh.isNullOrBlank() || !videoHLS.isNullOrBlank()
    if (!hasAnySource) return null

    val videoTitle = extractValue(trimmed, PATTERN_VIDEO_TITLE)
    val encodedIdVideo = extractValue(trimmed, PATTERN_ENCODED_ID)
    val thumbUrl = extractValue(trimmed, PATTERN_THUMB_URL)
    val thumbUrl169 = extractValue(trimmed, PATTERN_THUMB_URL_169)
    val thumbSlide = extractValue(trimmed, PATTERN_THUMB_SLIDE)
    val thumbSlideBig = extractValue(trimmed, PATTERN_THUMB_SLIDE_BIG)
    val thumbSlideMinute = extractValue(trimmed, PATTERN_THUMB_SLIDE_MINUTE)
    val idCDN = extractValue(trimmed, PATTERN_ID_CDN)
    val idCdnHLS = extractValue(trimmed, PATTERN_ID_CDN_HLS)
    val seekBarColor = extractValue(trimmed, PATTERN_SEEK_BAR_COLOR)
    val uploaderName = extractValue(trimmed, PATTERN_UPLOADER_NAME)
    val videoURL = extractValue(trimmed, PATTERN_VIDEO_URL)
    val staticPath = extractValue(trimmed, PATTERN_STATIC_PATH)
    val viewData = extractValue(trimmed, PATTERN_VIEW_DATA)

    return HTML5PlayerConfig(
        videoTitle = videoTitle ?: "",
        encodedIdVideo = encodedIdVideo ?: "",
        sponsors = emptyList(), // Sponsors parsing can be added similarly
        // X6: JS-строки экранируют слэши как "\/" — раскодируем, иначе URL не проигрываются.
        videoUrlLow = videoUrlLow.unescapeUrl(),
        videoUrlHigh = videoUrlHigh.unescapeUrl(),
        videoHLS = videoHLS.unescapeUrl(),
        thumbUrl = thumbUrl.unescapeUrl(),
        thumbUrl169 = thumbUrl169.unescapeUrl(),
        relatedVideos = null, // Placeholder for complex objects
        thumbSlide = thumbSlide.unescapeUrl(),
        thumbSlideBig = thumbSlideBig.unescapeUrl(),
        thumbSlideMinute = thumbSlideMinute.unescapeUrl(),
        idCDN = idCDN ?: "",
        idCdnHLS = idCdnHLS ?: "",
        fakePlayer = false, // Assuming default false
        desktopView = false, // Assuming default false
        seekBarColor = seekBarColor ?: "",
        uploaderName = uploaderName ?: "",
        videoURL = videoURL.unescapeUrl(),
        staticPath = staticPath.unescapeUrl(),
        viewData = viewData ?: ""
    )
}

/**
 * Безопасная перегрузка для nullable-строки скрипта.
 */
fun parseHTML5PlayerOrNull(script: String?): HTML5PlayerConfig? =
    if (script != null) parseHTML5Player(script) else null

/**
 * Быстро извлекает только название видеоролика из скрипта плеера.
 */
fun extractVideoTitle(script: String): String? {
    if (script.isBlank()) return null
    return extractValue(script, PATTERN_VIDEO_TITLE)
}

/**
 * Быстро извлекает тройку доступных URL видеопотоков (High, Low, HLS).
 */
fun extractVideoUrls(script: String): Triple<String, String, String> {
    if (script.isBlank()) return Triple("", "", "")
    val high = extractValue(script, PATTERN_URL_HIGH).unescapeUrl()
    val low = extractValue(script, PATTERN_URL_LOW).unescapeUrl()
    val hls = extractValue(script, PATTERN_URL_HLS).unescapeUrl()
    return Triple(high, low, hls)
}

/**
 * Проверяет, содержит ли скрипт хотя бы один пригодный к воспроизведению видеопоток.
 */
fun hasPlayableStream(script: String?): Boolean {
    if (script.isNullOrBlank()) return false
    val (high, low, hls) = extractVideoUrls(script)
    return high.isNotEmpty() || low.isNotEmpty() || hls.isNotEmpty()
}

/** Извлекает первое совпадение группы regex из текста скрипта. */
private fun extractValue(script: String, pattern: Pattern): String? {
    val matcher = pattern.matcher(script)
    return if (matcher.find()) matcher.group(1) else null
}

/** Декодирует экранированные слэши `\/` из JS-строк и нормализует URL. */
// X6: "https:\/\/cdn\/x.mp4" -> "https://cdn/x.mp4"; "//cdn..." -> "https://cdn..."; null -> "".
private fun String?.unescapeUrl(): String {
    if (this == null) return ""
    val trimmed = trim()
    if (trimmed.isEmpty()) return ""
    val unescaped = if (trimmed.contains("\\/")) trimmed.replace("\\/", "/") else trimmed
    return normalizeXUrl(unescaped)
}
