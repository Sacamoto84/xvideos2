package com.client.xvideos.calculator

import com.client.xvideos.common.ui.adaptive.WidthClass

/** Расстояние между кнопками и под каждым рядом — как в KeypadRow. */
internal const val KEYPAD_SPACING_DP = 12

/** Высота, которую клавиатура оставляет дисплею калькулятора. */
internal const val DISPLAY_RESERVE_DP = 160

/** Наибольшая ширина клавиатуры на широком окне. */
internal const val KEYPAD_MAX_WIDTH_DP = 420

/** Наименьшая кнопка — минимальный размер нажимаемого элемента Material. */
internal const val KEYPAD_MIN_BUTTON_DP = 48

private const val KEYPAD_COLUMNS = 4
private const val KEYPAD_ROWS = 5

/**
 * Ширина клавиатуры калькулятора или `null`, если ограничивать не надо.
 *
 * Кнопки квадратные и делят ширину на четыре, поэтому высота клавиатуры растёт
 * вместе с шириной: на раскрытом складном в альбомной ориентации клавиатура во
 * всю ширину не влезла бы по высоте. На телефоне — `null`, разметка прежняя.
 * На широком окне — не больше [KEYPAD_MAX_WIDTH_DP], не больше доступной
 * ширины и не больше ширины, при которой пять рядов помещаются в высоту,
 * оставляя дисплею [DISPLAY_RESERVE_DP]. В очень низком окне кнопка не
 * меньше [KEYPAD_MIN_BUTTON_DP]: без нижней границы клавиатура сжималась бы
 * в ноль, а так её нижние ряды остаются нажимаемыми, дисплей ужимается.
 */
fun keypadWidthDp(widthClass: WidthClass, availableWidthDp: Int, availableHeightDp: Int): Int? {
    if (widthClass == WidthClass.Compact) return null
    val keypadHeight = (availableHeightDp - DISPLAY_RESERVE_DP).coerceAtLeast(0)
    // Ряд = кнопка + отступ под ним; кнопка = (ширина − 3 отступа) / 4.
    val button = (keypadHeight / KEYPAD_ROWS - KEYPAD_SPACING_DP).coerceAtLeast(KEYPAD_MIN_BUTTON_DP)
    val byHeight = button * KEYPAD_COLUMNS + (KEYPAD_COLUMNS - 1) * KEYPAD_SPACING_DP
    return minOf(availableWidthDp, KEYPAD_MAX_WIDTH_DP, byHeight)
}
