package com.client.xvideos.common.util

import kotlin.math.round

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
    if (this.isNaN() || this.isInfinite() || this <= 0f) return "0:00"
    val totalHundredths = round(this * 100f).toLong()
    val whole = totalHundredths / 100L
    val frac = (totalHundredths % 100L).toInt()
    return if (frac < 10) "$whole:0$frac" else "$whole:$frac"
}
