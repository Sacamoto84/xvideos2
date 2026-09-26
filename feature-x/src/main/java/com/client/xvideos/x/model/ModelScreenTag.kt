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
    val hasTitle0: Boolean get() = title0.isNotBlank()
    val hasTitle1: Boolean get() = title1.isNotBlank()
    val displayTitle: String get() = title0.ifBlank { title1 }

    fun findByIdOrNull(id: Long): ItemsX? = if (id <= 0L) null else items.firstOrNull { it.id == id }


    companion object {
        val EMPTY = ModelScreenTag()
    }
}
