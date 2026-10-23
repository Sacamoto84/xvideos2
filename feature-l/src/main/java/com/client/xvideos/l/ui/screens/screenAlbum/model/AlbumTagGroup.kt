package com.client.xvideos.l.ui.screens.screenAlbum.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.common.util.capitalizeEachWord
import com.client.xvideos.l.model.Tag

/**
 * Теги альбома одной категории; [category] == null — теги без категории.
 *
 * Сайт отдаёт категорию дважды: в поле `category` и префиксом в тексте
 * («Character: frieren»). Раньше все теги шли одним рядом, и подпись категории
 * сливалась с первым чипом. Теперь категория — заголовок ряда, как «Audiences:»,
 * а в чипе — текст без префикса ([textWithoutCategory]).
 */
@Immutable
data class AlbumTagGroup(
    val category: String?,
    val tags: List<Tag>,
) {
    /** Заголовок ряда: категория, а у тегов без категории — «Tags». */
    val title: String get() = category?.capitalizeEachWord() ?: "Tags"
}

/** Группы в порядке первого появления категории; теги без категории — последней группой. */
fun groupAlbumTags(tags: List<Tag>): List<AlbumTagGroup> {
    val byCategory = tags.groupBy { it.category?.trim()?.takeIf(String::isNotEmpty) }
    val categorized = byCategory.mapNotNull { (category, list) -> category?.let { AlbumTagGroup(it, list) } }
    val plain = byCategory[null]?.let { listOf(AlbumTagGroup(null, it)) }.orEmpty()
    return categorized + plain
}

/** Текст тега без префикса «<категория>:»; без префикса или без значения после него — текст как есть. */
fun Tag.textWithoutCategory(): String {
    val prefix = category?.trim()?.takeIf(String::isNotEmpty) ?: return text
    if (!text.startsWith("$prefix:", ignoreCase = true)) return text
    return text.substring(prefix.length + 1).trim().ifEmpty { text }
}
