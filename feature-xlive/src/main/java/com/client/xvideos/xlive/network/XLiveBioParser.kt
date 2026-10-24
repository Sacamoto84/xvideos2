package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveBio
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import org.jsoup.Jsoup

/**
 * Анкета модели из ответа `api/biocontext/<ник>/`.
 *
 * @return `null`, если ответ не JSON-объект.
 */
fun parseXLiveBio(json: String): XLiveBio? {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null
    return XLiveBio(
        realName = root.field("real_name"),
        age = root.primitive("display_age")?.intOrNull,
        location = root.field("location"),
        languages = root.field("languages"),
        bodyType = root.field("body_type"),
        interestedIn = (root["interested_in"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            .orEmpty(),
        followers = root.primitive("follower_count")?.intOrNull,
        aboutMe = htmlToText(root.string("about_me").orEmpty()),
    )
}

// `<br>` сюда не входит: wholeText() сам делает из него перенос строки.
/** Поле анкеты без пробелов и запятой на краях: сайт отдаёт списки вида `Slim, Fit,`. */
private fun JsonObject.field(key: String): String = string(key).orEmpty().trim().trim(',').trim()

private val LINE_BREAK = Regex("""(?i)</p>|</div>|</li>""")
private val BLANK_LINES = Regex("""\n[ \t]*\n(?:[ \t]*\n)+""")

/**
 * «О себе» сайта — HTML с абзацами, переносами и ссылками — в простой текст.
 *
 * Переносы строк сохраняются, ссылки остаются своим текстом, три и больше
 * пустых строк подряд сжимаются до одной.
 */
internal fun htmlToText(html: String): String {
    if (html.isBlank()) return ""
    val withBreaks = html.replace(LINE_BREAK) { "${it.value}\n" }
    return Jsoup.parseBodyFragment(withBreaks).body().wholeText()
        .lines()
        .joinToString("\n") { it.trim() }
        .replace(BLANK_LINES, "\n\n")
        .trim()
}
