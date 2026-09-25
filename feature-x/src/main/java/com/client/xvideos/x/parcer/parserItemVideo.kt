package com.client.xvideos.x.parcer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Признак нужного скрипта: по нему его и ищем. */
private const val PLAYER_MARKER = "html5player."

/**
 * Достаёт из DOM страницы скрипт с конфигом html5-плеера. `null` — не нашёлся.
 */
fun parserItemVideo(document: Document): String? {
    val container = document.selectFirst("#video-player-bg")
    if (container != null) {
        val script = container.getElementsByTag("script").firstOrNull { it.data().contains(PLAYER_MARKER) }
        if (script != null) return script.data()
    }

    return document.getElementsByTag("script")
        .firstOrNull { it.data().contains(PLAYER_MARKER) }
        ?.data()
}

/**
 * Достаёт из HTML строки скрипт с конфигом html5-плеера. `null` — не нашёлся.
 */
fun parserItemVideo(html: String): String? {
    if (html.isBlank() || !html.contains(PLAYER_MARKER)) return null
    return parserItemVideo(Jsoup.parse(html))
}
