package com.client.xvideos.x.parcer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Признак нужного скрипта: по нему его и ищем. */
private const val PLAYER_MARKER = "html5player."

/**
 * Извлекает тело JavaScript-скрипта инициализации HTML5-плеера из DOM-дерева страницы видео.
 *
 * Поиск оптимизирован: сначала проверяется блок контейнера `#video-player-bg`,
 * затем выполняется поиск по всем скриптам документа.
 *
 * @param document Разобранный HTML-документ Jsoup.
 * @return Текст скрипта инициализации плеера либо `null`, если скрипт не найден.
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
 * Извлекает тело JavaScript-скрипта инициализации HTML5-плеера из сырой HTML-строки.
 *
 * @param html Текст HTML-страницы ролика.
 * @return Текст скрипта инициализации плеера либо `null`.
 */
fun parserItemVideo(html: String): String? {
    if (html.isBlank() || !html.contains(PLAYER_MARKER)) return null
    return parserItemVideo(Jsoup.parse(html))
}

/**
 * Быстрая проверка наличия маркера скрипта плеера в HTML-разметке без полного парсинга DOM.
 */
fun hasPlayerScript(html: String?): Boolean =
    !html.isNullOrBlank() && html.contains(PLAYER_MARKER)

/**
 * Проверяет наличие маркера плеера в произвольном тексте.
 */
fun hasPlayerMarker(text: String?): Boolean =
    !text.isNullOrBlank() && text.contains(PLAYER_MARKER)

/**
 * Разбивает тело скрипта плеера на строки вызовов `html5player.set*`.
 */
fun extractPlayerScriptLines(script: String?): List<String> {
    if (script.isNullOrBlank()) return emptyList()
    return script.lines()
        .map { it.trim() }
        .filter { it.startsWith("html5player.") }
}

/**
 * Подсчитывает количество параметров инициализации `html5player.set*` в скрипте.
 */
fun countPlayerProperties(script: String?): Int =
    extractPlayerScriptLines(script).size

