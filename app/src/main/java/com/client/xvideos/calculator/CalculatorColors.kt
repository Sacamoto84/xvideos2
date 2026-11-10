package com.client.xvideos.calculator

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Цветовая палитра калькулятора в фирменном светлом стиле Xiaomi (MIUI / HyperOS Light Theme).
 */
object CalculatorColors {
    // Основной фон экрана
    val XIAOMI_BG = Color(0xFFFFFFFF)

    // Фирменный оранжевый акцент Xiaomi (операторы, клавиша =, активные значения)
    val XIAOMI_ORANGE = Color(0xFFFF6900)
    val XIAOMI_ORANGE_PRESSED = Color(0xFFE55D00)

    // Клавиши калькулятора в светлой теме
    val XIAOMI_KEY_BG = Color(0xFFF7F7F7)
    val XIAOMI_KEY_DIGIT_TEXT = Color(0xFF1C1C1E)
    val XIAOMI_KEY_SCIENTIFIC = Color(0xFFF0F0F0)

    // Карточки, разделители и поверхности
    val XIAOMI_SURFACE = Color(0xFFF7F7F7)
    val XIAOMI_SURFACE_VARIANT = Color(0xFFEFEFEF)
    val XIAOMI_DIVIDER = Color(0xFFE5E5E5)

    // Текстовые цвета
    val XIAOMI_TEXT_PRIMARY = Color(0xFF1C1C1E)
    val XIAOMI_TEXT_SECONDARY = Color(0xFF8E8E93)
    val XIAOMI_TEXT_MUTED = Color(0xFFB0B0B0)

    // Курсор ввода (синяя вертикальная черточка)
    val XIAOMI_CURSOR = Color(0xFF0066FF)

    // Совместимость со старыми вызовами и существующими тестами
    val XIAOMI_DARK_BG = XIAOMI_BG
    val FUNCTION_BG = XIAOMI_KEY_BG
    val FUNCTION_TEXT = XIAOMI_ORANGE
    val DIGIT_BG = XIAOMI_KEY_BG
    val DIGIT_TEXT = XIAOMI_KEY_DIGIT_TEXT
    val OPERATOR_BG = XIAOMI_ORANGE
    val OPERATOR_TEXT = Color.White
    val DISPLAY_HORIZONTAL_PADDING = 16.dp
}
