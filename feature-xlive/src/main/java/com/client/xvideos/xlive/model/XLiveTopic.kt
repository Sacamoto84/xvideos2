package com.client.xvideos.xlive.model

/**
 * Тема комнаты, разобранная на текст и теги.
 *
 * @property text тема без хештегов; пустая, если в ней были только теги.
 * @property tags теги без `#`, в порядке темы, без повторов.
 */
data class XLiveTopic(val text: String, val tags: List<String>)

private val HASHTAG = Regex("""#([\p{L}\p{N}_]+)""")
private val SPACES = Regex("""\s{2,}""")

/** Разделители, которые повисают на краях темы, когда теги из неё убраны. */
private val EDGE_SEPARATORS = Regex("""^[\s\-|•·,]+|[\s\-|•·,]+$""")

/**
 * Тема комнаты [title] — текст отдельно, теги отдельно.
 *
 * Сайт пишет теги прямо в теме (`toe suck #petite #ginger`), а отдельного
 * списка в данных комнаты нет. Повторы отбрасываются без учёта регистра.
 * Разделитель перед тегами (`goal ! - #a #b`) после их удаления тоже уходит.
 */
fun splitRoomTitle(title: String): XLiveTopic {
    val tags = HASHTAG.findAll(title)
        .map { it.groupValues[1] }
        .distinctBy { it.lowercase() }
        .toList()
    val text = title.replace(HASHTAG, " ").replace(SPACES, " ").replace(EDGE_SEPARATORS, "")
    return XLiveTopic(text = text, tags = tags)
}
