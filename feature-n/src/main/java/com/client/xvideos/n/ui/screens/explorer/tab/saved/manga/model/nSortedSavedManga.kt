package com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.model

import com.client.xvideos.n.model.NGalleryPreview

/**
 * Сортирует список сохранённой манги в соответствии с выбранным [order].
 */
fun nSortedSavedManga(items: List<NGalleryPreview>, order: NSavedMangaOrder): List<NGalleryPreview> {
    return when (order) {
        NSavedMangaOrder.NEWEST -> items.reversed()
        NSavedMangaOrder.OLDEST -> items
        NSavedMangaOrder.TITLE -> items.sortedBy { it.title.lowercase() }
        NSavedMangaOrder.MOST_PAGES -> items.sortedByDescending { it.numPages }
    }
}
