package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.normalizeXUrl
import java.util.regex.Pattern

/**
 * Вызов `html5player.<setter>('...')`. Значение — тело JS-строки в одинарных кавычках
 * вместе с escape-последовательностями: прежний `(.*?)` обрывался на `\')` внутри
 * названия. Раскодирует [extractValue].
 */
private fun setterPattern(setter: String): Pattern =
    Pattern.compile("""html5player\.$setter\('((?:[^'\\]|\\.)*)'\)""")

private val PATTERN_VIDEO_TITLE = setterPattern("setVideoTitle")
private val PATTERN_ENCODED_ID = setterPattern("setEncodedIdVideo")
private val PATTERN_URL_LOW = setterPattern("setVideoUrlLow")
private val PATTERN_URL_HIGH = setterPattern("setVideoUrlHigh")
private val PATTERN_URL_HLS = setterPattern("setVideoHLS")
private val PATTERN_THUMB_URL = setterPattern("setThumbUrl")
private val PATTERN_THUMB_URL_169 = setterPattern("setThumbUrl169")
private val PATTERN_THUMB_SLIDE = setterPattern("setThumbSlide")
private val PATTERN_THUMB_SLIDE_BIG = setterPattern("setThumbSlideBig")
private val PATTERN_THUMB_SLIDE_MINUTE = setterPattern("setThumbSlideMinute")
private val PATTERN_ID_CDN = setterPattern("setIdCDN")
private val PATTERN_ID_CDN_HLS = setterPattern("setIdCdnHLS")
private val PATTERN_SEEK_BAR_COLOR = setterPattern("setSeekBarColor")
private val PATTERN_UPLOADER_NAME = setterPattern("setUploaderName")
private val PATTERN_VIDEO_URL = setterPattern("setVideoURL")
private val PATTERN_STATIC_PATH = setterPattern("setStaticPath")
private val PATTERN_VIEW_DATA = setterPattern("setViewData")

private val JS_ESCAPE = Regex("""\\(?:x([0-9A-Fa-f]{2})|u([0-9A-Fa-f]{4})|(.))""")

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

/**
 * Извлекает наиболее приоритетный доступный URL потока воспроизведения (High -> Low -> HLS).
 */
fun extractPrimaryStreamUrl(script: String): String {
    val (high, low, hls) = extractVideoUrls(script)
    return high.ifEmpty { low.ifEmpty { hls } }
}

/** Извлекает значение первого вызова сеттера плеера с раскодированными JS-escape. */
private fun extractValue(script: String, pattern: Pattern): String? {
    val matcher = pattern.matcher(script)
    return if (matcher.find()) unescapeJsString(matcher.group(1)) else null
}

/**
 * Раскодирует тело строкового литерала JavaScript: `\'`, `\"`, `\\`, `\/`, `\n`, `\r`,
 * `\t`, `\xHH`, `\uHHHH`. Прочий символ после `\` остаётся без слэша, как в JS.
 */
internal fun unescapeJsString(raw: String): String {
    if ('\\' !in raw) return raw
    return JS_ESCAPE.replace(raw) { match ->
        val hex = match.groups[1]?.value ?: match.groups[2]?.value
        if (hex != null) {
            hex.toInt(16).toChar().toString()
        } else {
            when (val escaped = match.groupValues[3]) {
                "n" -> "\n"
                "r" -> "\r"
                "t" -> "\t"
                else -> escaped
            }
        }
    }
}

/** Нормализует URL; JS-экранирование (`\/`) уже снято в [extractValue]. */
// X6: "https:\/\/cdn\/x.mp4" -> "https://cdn/x.mp4"; "//cdn..." -> "https://cdn..."; null -> "".
private fun String?.unescapeUrl(): String = normalizeXUrl(this.orEmpty())
