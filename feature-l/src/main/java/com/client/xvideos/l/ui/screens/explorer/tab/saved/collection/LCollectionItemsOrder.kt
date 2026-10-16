package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

/**
 * Порядок элементов открытой коллекции; [title] — пункт меню сортировки.
 * Список, а не флаг: в меню могут добавиться новые варианты.
 */
enum class LCollectionItemsOrder(val title: String) {
    NEWEST("Сначала новые"),
    OLDEST("Сначала старые"),
}
