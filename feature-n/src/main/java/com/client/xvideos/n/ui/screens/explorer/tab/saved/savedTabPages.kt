package com.client.xvideos.n.ui.screens.explorer.tab.saved

/**
 * Количество отображаемых вкладок в разделе Saved модуля N.
 *
 * Вкладки 0–2 (лайки страниц, сохранённая манга, коллекции) работают локально.
 * Вкладка 3 (избранное на сервере) отображается только при активной авторизации.
 */
internal fun nSavedTabPageCount(isLoggedIn: Boolean): Int = if (isLoggedIn) 4 else 3

/**
 * Ограничивает выбранный индекс страницы допустимым диапазоном видимых вкладок.
 */
internal fun nSavedTabPage(page: Int, pageCount: Int): Int = page.coerceIn(0, pageCount - 1)
