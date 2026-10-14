package com.client.xvideos.common.ui.adaptive

/**
 * Класс ширины окна. Границы — стандартные для Material: 600 и 840dp.
 *
 * Compact — телефон и внешний экран складного; Medium и Expanded — внутренний
 * экран складного в портрете и в альбомной ориентации. Вся адаптация
 * раскладки включается только начиная с [Medium].
 */
enum class WidthClass { Compact, Medium, Expanded }

const val MEDIUM_MIN_WIDTH_DP = 600
const val EXPANDED_MIN_WIDTH_DP = 840

fun widthClassOf(widthDp: Int): WidthClass = when {
    widthDp >= EXPANDED_MIN_WIDTH_DP -> WidthClass.Expanded
    widthDp >= MEDIUM_MIN_WIDTH_DP -> WidthClass.Medium
    else -> WidthClass.Compact
}
