package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

/**
 * Порядок сохранённых альбомов L; [title] — пункт меню сортировки.
 * Номер в списке хранится в настройках — новые варианты добавлять в конец.
 */
enum class LSavedAlbumsOrder(val title: String) {
    NEWEST("Сначала новые"),
    OLDEST("Сначала старые"),
    MOST_ITEMS("Больше элементов"),
}
