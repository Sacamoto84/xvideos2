package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import com.client.xvideos.l.model.PicsDetails

/**
 * Элементы открытой коллекции с отбором по типу [type] и порядком [order].
 *
 * [items] приходят с диска от новых к старым (`lReadCollectionItems` сортирует
 * по времени сохранения), поэтому «сначала старые» — тот же список задом наперёд.
 * Gif — и анимированные картинки, и элементы с видео.
 */
fun lCollectionVisibleItems(
    items: List<PicsDetails>,
    type: LCollectionTypeFilter,
    order: LCollectionItemsOrder,
): List<PicsDetails> {
    val byType = when (type) {
        LCollectionTypeFilter.ALL -> items
        LCollectionTypeFilter.IMAGES -> items.filterNot { it.isGifOrVideo }
        LCollectionTypeFilter.GIFS -> items.filter { it.isGifOrVideo }
    }
    return when (order) {
        LCollectionItemsOrder.NEWEST -> byType
        LCollectionItemsOrder.OLDEST -> byType.reversed()
    }
}
