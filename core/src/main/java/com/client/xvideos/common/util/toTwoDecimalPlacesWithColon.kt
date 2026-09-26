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
private val TWO_DIGIT_FRACS: Array<String> = Array(100) { frac ->
    if (frac < 10) "0$frac" else "$frac"
}

fun Float.toTwoDecimalPlacesWithColon(): String {
    if (this.isNaN() || this.isInfinite() || this <= 0f) return "0:00"
    val totalHundredths = round(this * 100f).toLong()
    val whole = totalHundredths / 100L
    val frac = (totalHundredths % 100L).toInt()
    val fracStr = if (frac in 0..99) TWO_DIGIT_FRACS[frac] else if (frac < 10) "0$frac" else "$frac"
    return "$whole:$fracStr"
}

fun Double.toTwoDecimalPlacesWithColon(): String =
    if (this.isNaN() || this.isInfinite() || this <= 0.0) "0:00" else this.toFloat().toTwoDecimalPlacesWithColon()

fun Int.toTwoDecimalPlacesWithColon(): String =
    if (this <= 0) "0:00" else "$this:00"

fun Long.toTwoDecimalPlacesWithColon(): String =
    if (this <= 0L) "0:00" else "$this:00"

fun String?.toTwoDecimalPlacesWithColonOrDefault(default: String = "0:00"): String =
    this?.toFloatOrNull()?.toTwoDecimalPlacesWithColon() ?: default


