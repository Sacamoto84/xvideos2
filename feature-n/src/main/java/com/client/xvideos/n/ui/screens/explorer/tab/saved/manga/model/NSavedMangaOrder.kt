package com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.model

/**
 * Порядок сортировки локально сохранённой манги.
 */
enum class NSavedMangaOrder(val title: String) {
    NEWEST("Сначала новые"),
    OLDEST("Сначала старые"),
    TITLE("По названию"),
    MOST_PAGES("Больше страниц")
}
