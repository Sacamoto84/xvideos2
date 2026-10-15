package com.client.xvideos.x.screens.common.bottomKeyboard

import com.client.xvideos.common.ui.adaptive.WidthClass

/**
 * Сколько номеров страниц видно в ленте одновременно.
 *
 * На телефоне — 5, как было. На широком окне лента идёт на всю ширину, и номеров
 * больше, чтобы кнопки не растягивались. Число нечётное: текущая страница
 * встаёт по центру ленты.
 */
internal fun visiblePageNumbers(widthClass: WidthClass): Int = when (widthClass) {
    WidthClass.Compact -> 5
    WidthClass.Medium -> 9
    WidthClass.Expanded -> 11
}
