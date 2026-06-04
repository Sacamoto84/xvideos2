package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.HTML5PlayerConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

fun parseHTML5Player(script: String): HTML5PlayerConfig {
    val videoTitle = extractValue(script, "html5player.setVideoTitle\\('(.*?)'\\)")
    val encodedIdVideo = extractValue(script, "html5player.setEncodedIdVideo\\('(.*?)'\\)")
    val videoUrlLow = extractValue(script, "html5player.setVideoUrlLow\\('(.*?)'\\)")
    val videoUrlHigh = extractValue(script, "html5player.setVideoUrlHigh\\('(.*?)'\\)")
    val videoHLS = extractValue(script, "html5player.setVideoHLS\\('(.*?)'\\)")
    val thumbUrl = extractValue(script, "html5player.setThumbUrl\\('(.*?)'\\)")
    val thumbUrl169 = extractValue(script, "html5player.setThumbUrl169\\('(.*?)'\\)")
    val thumbSlide = extractValue(script, "html5player.setThumbSlide\\('(.*?)'\\)")
    val thumbSlideBig = extractValue(script, "html5player.setThumbSlideBig\\('(.*?)'\\)")
    val thumbSlideMinute = extractValue(script, "html5player.setThumbSlideMinute\\('(.*?)'\\)")
    val idCDN = extractValue(script, "html5player.setIdCDN\\('(.*?)'\\)")
    val idCdnHLS = extractValue(script, "html5player.setIdCdnHLS\\('(.*?)'\\)")
    val seekBarColor = extractValue(script, "html5player.setSeekBarColor\\('(.*?)'\\)")
    val uploaderName = extractValue(script, "html5player.setUploaderName\\('(.*?)'\\)")
    val videoURL = extractValue(script, "html5player.setVideoURL\\('(.*?)'\\)")
    val staticPath = extractValue(script, "html5player.setStaticPath\\('(.*?)'\\)")
    val viewData = extractValue(script, "html5player.setViewData\\('(.*?)'\\)")

    return HTML5PlayerConfig(
        videoTitle = videoTitle ?: "",
        encodedIdVideo = encodedIdVideo ?: "",
        sponsors = listOf(), // Sponsors parsing can be added similarly
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

// X5: компилируем каждый паттерн один раз (на видео их ~20, и функция зовётся часто).
private val patternCache = ConcurrentHashMap<String, Pattern>()

private fun extractValue(script: String, pattern: String): String? {
    val regex = patternCache.getOrPut(pattern) { Pattern.compile(pattern) }
    val matcher = regex.matcher(script)
    return if (matcher.find()) matcher.group(1) else null
}

// X6: "https:\/\/cdn\/x.mp4" -> "https://cdn/x.mp4"; null -> "".
private fun String?.unescapeUrl(): String = this?.replace("\\/", "/") ?: ""
