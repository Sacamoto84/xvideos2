package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable

/**
 * Разобранная страница выдачи по тегу раздела X.
 *
 * @property title0 Основной заголовок экрана тега.
 * @property title1 Вспомогательный/альтернативный заголовок экрана тега.
 * @property items Список видеороликов [ItemsX], найденных по тегу на текущей странице.
 * @property lastPage Число страниц выдачи. Считается с единицы; `1` — страница одна либо блок пагинации отсутствует.
 */
@Immutable
data class ModelScreenTag(
    val title0: String = "",
    val title1: String = "",
    val items: List<ItemsX> = emptyList(),
    val lastPage: Int = 1,
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val size: Int get() = items.size
    val count: Int get() = items.size
    val firstOrNull: ItemsX? get() = items.firstOrNull()
    val hasMultiplePages: Boolean get() = lastPage > 1
    val hasPagination: Boolean get() = hasMultiplePages
    val hasTitle0: Boolean get() = title0.isNotBlank()
    val hasTitle1: Boolean get() = title1.isNotBlank()
    val displayTitle: String get() = title0.ifBlank { title1 }

    fun findByIdOrNull(id: Long): ItemsX? = if (id <= 0L) null else items.firstOrNull { it.id == id }
    fun hasItemWithId(id: Long): Boolean = findByIdOrNull(id) != null

    fun filterByQuery(query: String?): List<ItemsX> {
        if (query.isNullOrBlank()) return items
        return items.filter { it.matches(query) }
    }

    val normalizedTitle: String get() = displayTitle.trim()

    /** Проверяет соответствие заголовков или вложенных видео поисковому запросу. */
    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val q = query.trim()
        return title0.contains(q, ignoreCase = true) ||
            title1.contains(q, ignoreCase = true) ||
            items.any { it.matches(q) }
    }

    companion object {
        val EMPTY = ModelScreenTag()
    }
}
