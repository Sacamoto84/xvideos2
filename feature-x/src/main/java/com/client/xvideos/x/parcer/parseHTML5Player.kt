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
 * Разбирает скрипт html5-плеера. `null` — играть нечего.
 *
 * Раньше при полном промахе отсюда уходил `HTML5PlayerConfig` со всеми полями
 * `""`: отказ выглядел как успех с пустыми данными, плеер получал пустые адреса
 * и молчал. Тот же класс дефекта, что чинили в кеше лент R.
 *
 * Признак «нечего играть» — ни одного источника: ни низкого качества, ни
 * высокого, ни HLS. Всё остальное (название, превью, имя автора) может
 * отсутствовать на законных основаниях и разбор не отменяет.
 */
fun parseHTML5Player(script: String): HTML5PlayerConfig? {
    if (script.isBlank()) return null

    val videoTitle = extractValue(script, PATTERN_VIDEO_TITLE)
    val encodedIdVideo = extractValue(script, PATTERN_ENCODED_ID)
    val videoUrlLow = extractValue(script, PATTERN_URL_LOW)
    val videoUrlHigh = extractValue(script, PATTERN_URL_HIGH)
    val videoHLS = extractValue(script, PATTERN_URL_HLS)
    val thumbUrl = extractValue(script, PATTERN_THUMB_URL)
    val thumbUrl169 = extractValue(script, PATTERN_THUMB_URL_169)
    val thumbSlide = extractValue(script, PATTERN_THUMB_SLIDE)
    val thumbSlideBig = extractValue(script, PATTERN_THUMB_SLIDE_BIG)
    val thumbSlideMinute = extractValue(script, PATTERN_THUMB_SLIDE_MINUTE)
    val idCDN = extractValue(script, PATTERN_ID_CDN)
    val idCdnHLS = extractValue(script, PATTERN_ID_CDN_HLS)
    val seekBarColor = extractValue(script, PATTERN_SEEK_BAR_COLOR)
    val uploaderName = extractValue(script, PATTERN_UPLOADER_NAME)
    val videoURL = extractValue(script, PATTERN_VIDEO_URL)
    val staticPath = extractValue(script, PATTERN_STATIC_PATH)
    val viewData = extractValue(script, PATTERN_VIEW_DATA)

    val hasAnySource = !videoUrlLow.isNullOrBlank() || !videoUrlHigh.isNullOrBlank() || !videoHLS.isNullOrBlank()
    if (!hasAnySource) return null

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

private fun extractValue(script: String, pattern: Pattern): String? {
    val matcher = pattern.matcher(script)
    return if (matcher.find()) matcher.group(1) else null
}

// X6: "https:\/\/cdn\/x.mp4" -> "https://cdn/x.mp4"; "//cdn..." -> "https://cdn..."; null -> "".
private fun String?.unescapeUrl(): String {
    if (this == null) return ""
    val unescaped = if (contains("\\/")) replace("\\/", "/").trim() else trim()
    return if (unescaped.isBlank()) "" else normalizeXUrl(unescaped)
}
