package com.client.xvideos.common.collectionDB.model

import androidx.compose.runtime.Immutable

/**
 * Универсальное представление одной коллекции в гриде (общее для L и R).
 *
 * @param name             отображаемое имя коллекции (оно же id папки)
 * @param previewUrl       url или локальный путь к превью; null — серая заглушка
 * @param itemsCount       подпись со счётчиком; null — счётчик не показывать
 */
@Immutable
data class CollectionGridItem(
    val name: String,
    val previewUrl: String?,
    val itemsCount: Int?
) {
    val isValid: Boolean get() = name.isNotBlank()
    val hasPreview: Boolean get() = !previewUrl.isNullOrBlank()
    val hasCount: Boolean get() = itemsCount != null
    val hasPositiveCount: Boolean get() = itemsCount != null && itemsCount > 0

    companion object {
        val EMPTY = CollectionGridItem(name = "", previewUrl = null, itemsCount = null)
    }
}
