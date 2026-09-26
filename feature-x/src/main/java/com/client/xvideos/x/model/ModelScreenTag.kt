package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable

/**
 * Разобранная страница тега.
 *
 * @param lastPage число страниц выдачи. Считается с единицы; `1` — страница
 *   одна либо блок постраничности на странице отсутствует.
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
    val hasMultiplePages: Boolean get() = lastPage > 1

    companion object {
        val EMPTY = ModelScreenTag()
    }
}
