package com.client.xvideos.x.parcer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Признак нужного скрипта: по нему его и ищем. */
private const val PLAYER_MARKER = "html5player."

/**
 * Достаёт из страницы скрипт с конфигом html5-плеера. `null` — не нашёлся.
 *
 * Раньше скрипт брался по месту: `#video-player-bg > script:nth-child(6)`.
 * Шестой по счёту. Вставят на сайте один тег выше — `select` вернёт пустую
 * коллекцию, `html()` отдаст пустую строку, и разбор пойдёт дальше как ни в чём
 * не бывало: плеер получит конфиг с пустыми адресами и промолчит.
 *
 * Ищем по содержимому. Контейнер по-прежнему предпочитаем — он сужает поиск и
 * отсекает чужие скрипты, — но если разметка поменялась, обходим все скрипты
 * страницы.
 */
fun parserItemVideo(html: String): String? {
    val document: Document = Jsoup.parse(html)

    val inContainer = document.select("#video-player-bg script")
        .firstOrNull { it.data().contains(PLAYER_MARKER) }
    if (inContainer != null) return inContainer.data()

    return document.select("script")
        .firstOrNull { it.data().contains(PLAYER_MARKER) }
        ?.data()
}
