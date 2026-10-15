package com.client.xvideos.l.ui.screens.explorer.tab.saved

/**
 * Сколько вкладок у Saved L.
 *
 * Вкладки 0–2 (лайки, альбомы, коллекции) локальные. Вкладки 3 и 4 (подписки и
 * лайки на сервере) работают только с аккаунтом L — без него их нет совсем.
 * Аккаунт — сохранённые логин и пароль, как для формы входа и строки профиля
 * в настройках.
 */
internal fun savedTabPageCount(hasAccount: Boolean): Int = if (hasAccount) 5 else 3

/**
 * Страница [page] в пределах [pageCount] вкладок: после выхода из аккаунта
 * Saved не остаётся на скрытой серверной вкладке.
 */
internal fun savedTabPage(page: Int, pageCount: Int): Int = page.coerceIn(0, pageCount - 1)
