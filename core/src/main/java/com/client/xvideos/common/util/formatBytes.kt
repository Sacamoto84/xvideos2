package com.client.xvideos.common.util

import kotlin.math.roundToInt

private const val ONE_KB = 1024L
private const val ONE_MB = 1024L * 1024L
private const val ONE_GB = 1024L * 1024L * 1024L

private const val KB_DOUBLE = 1024.0
private const val MB_DOUBLE = 1024.0 * 1024.0
private const val GB_DOUBLE = 1024.0 * 1024.0 * 1024.0

/**
 * Форматирует объем данных в человекочитаемую строку с автоматическим выбором единиц (B, KB, MB, GB).
 *
 * @param bytes Размер данных в байтах.
 * @return Форматированная строка (например, "12.4 MB" или "1.25 GB").
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < ONE_KB) return "$bytes B"
    return when {
        bytes < ONE_MB -> "${(bytes / KB_DOUBLE).roundToInt()} KB"
        bytes < ONE_GB -> "${(bytes / MB_DOUBLE * 10).roundToInt() / 10.0} MB"
        else -> "${(bytes / GB_DOUBLE * 100).roundToInt() / 100.0} GB"
    }
}

/** Расширение для форматирования [Long] байтов. */
fun Long.formatAsBytes(): String = formatBytes(this)

/** Расширение для форматирования [Int] байтов. */
fun Int.formatAsBytes(): String = formatBytes(this.toLong())

/** Расширение для форматирования [Double] байтов с безопасной обработкой NaN/Infinity. */
fun Double.formatAsBytes(): String =
    if (this.isNaN() || this.isInfinite() || this <= 0.0) "0 B" else formatBytes(this.toLong())

/** Расширение для форматирования [Float] байтов с безопасной обработкой NaN/Infinity. */
fun Float.formatAsBytes(): String =
    if (this.isNaN() || this.isInfinite() || this <= 0f) "0 B" else formatBytes(this.toLong())

/** Форматирует [Long]? байтов или возвращает [default], если значение null. */
fun formatBytesOrDefault(bytes: Long?, default: String = "0 B"): String =
    if (bytes == null) default else formatBytes(bytes)

/** Проверяет, соответствует ли строка формату отформатированного объема байт (B, KB, MB, GB). */
fun isValidByteString(formatted: String?): Boolean {
    if (formatted.isNullOrBlank()) return false
    val trimmed = formatted.trim()
    return trimmed.endsWith(" B") || trimmed.endsWith(" KB") || trimmed.endsWith(" MB") || trimmed.endsWith(" GB")
}

