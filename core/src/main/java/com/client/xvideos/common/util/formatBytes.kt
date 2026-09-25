package com.client.xvideos.common.util

import kotlin.math.roundToInt

private const val ONE_KB = 1024L
private const val ONE_MB = 1024L * 1024L
private const val ONE_GB = 1024L * 1024L * 1024L

private const val KB_DOUBLE = 1024.0
private const val MB_DOUBLE = 1024.0 * 1024.0
private const val GB_DOUBLE = 1024.0 * 1024.0 * 1024.0

// Функция для форматирования объема данных
fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < ONE_KB) return "$bytes B"
    return when {
        bytes < ONE_MB -> "${(bytes / KB_DOUBLE).roundToInt()} KB"
        bytes < ONE_GB -> "${(bytes / MB_DOUBLE * 10).roundToInt() / 10.0} MB"
        else -> "${(bytes / GB_DOUBLE * 100).roundToInt() / 100.0} GB"
    }
}
