package com.client.xvideos.common.ui.adaptive

/** Колонки сетки, у которой на телефоне их две. */
fun gridColumns(widthClass: WidthClass): Int = when (widthClass) {
    WidthClass.Compact -> 2
    WidthClass.Medium -> 3
    WidthClass.Expanded -> 4
}

/**
 * Колонки сетки, где число выбирает пользователь: выбор плюс запас широкого
 * окна. Выбор хранится как есть — прибавка только при отрисовке.
 */
fun userGridColumns(selected: Int, widthClass: WidthClass): Int = selected + when (widthClass) {
    WidthClass.Compact -> 0
    WidthClass.Medium -> 1
    WidthClass.Expanded -> 2
}

/** Сколько карточек горизонтального ряда видно целиком. */
fun rowVisibleItems(widthClass: WidthClass): Int = when (widthClass) {
    WidthClass.Compact -> 3
    WidthClass.Medium -> 4
    WidthClass.Expanded -> 5
}

/** Наибольшая ширина одноколоночного контента: списков, шапок, рядов панелей. */
const val CONTENT_MAX_WIDTH_DP = 640

/** Отступ списка с каждой стороны: 0, пока контейнер не шире [CONTENT_MAX_WIDTH_DP]. */
fun contentSidePaddingDp(containerWidthDp: Int): Int =
    ((containerWidthDp - CONTENT_MAX_WIDTH_DP) / 2).coerceAtLeast(0)
