package com.client.xvideos.common.util

import java.util.Locale

/**
 * Форматирует Float до двух знаков после разделителя (который будет заменен на двоеточие),
 * всегда отображая два знака, даже если они нули.
 *
 * Примеры:
 * ```kotlin
 * 2.984f.toTwoDecimalPlacesWithColon()  // "2:98"
 * 2.0f.toTwoDecimalPlacesWithColon()    // "2:00"
 * 10f.toTwoDecimalPlacesWithColon()     // "10:00"
 * 123.456f.toTwoDecimalPlacesWithColon()// "123:46"
 * ```
 */
fun Float.toTwoDecimalPlacesWithColon(): String {
    // Сначала форматируем с точкой как разделителем
    val formattedWithDot = String.format(Locale.US, "%.2f", this)
    // Затем заменяем точку на двоеточие
    return formattedWithDot.replace('.', ':')
}