package com.client.xvideos.x.parcer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Признак нужного скрипта: по нему его и ищем. */
private const val PLAYER_MARKER = "html5player."

/**
 * Достаёт из DOM страницы скрипт с конфигом html5-плеера. `null` — не нашёлся.
 */
fun parserItemVideo(document: Document): String? {
    val inContainer = document.select("#video-player-bg script")
        .firstOrNull { it.data().contains(PLAYER_MARKER) }
    if (inContainer != null) return inContainer.data()

    return document.select("script")
        .firstOrNull { it.data().contains(PLAYER_MARKER) }
        ?.data()
}

/**
 * Достаёт из HTML строки скрипт с конфигом html5-плеера. `null` — не нашёлся.
 */
fun parserItemVideo(html: String): String? {
    return parserItemVideo(Jsoup.parse(html))
}
