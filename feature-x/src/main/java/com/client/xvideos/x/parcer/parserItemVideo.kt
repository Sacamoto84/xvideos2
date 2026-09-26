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
        for (script in container.getElementsByTag("script")) {
            val data = script.data()
            if (data.contains(PLAYER_MARKER)) return data
        }
    }

    for (script in document.getElementsByTag("script")) {
        if (container != null && script.parent() === container) continue
        val data = script.data()
        if (data.contains(PLAYER_MARKER)) return data
    }
    return null
}

/**
 * Достаёт из HTML строки скрипт с конфигом html5-плеера. `null` — не нашёлся.
 */
fun parserItemVideo(html: String): String? {
    if (html.isBlank() || !html.contains(PLAYER_MARKER)) return null
    return parserItemVideo(Jsoup.parse(html))
}
