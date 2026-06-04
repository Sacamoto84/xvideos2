package com.client.xvideos.x.parcer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun parserItemVideo(html: String): String? {
    val document: Document = Jsoup.parse(html)
    // Скрипт с конфигом html5-плеера.
    return document.select("#video-player-bg > script:nth-child(6)").html()
}


