package com.client.xvideos.common.util

import kotlin.math.roundToInt

// Функция для форматирования объема данных
fun formatBytes(bytes: Long): String {
    return when {
        bytes < 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${(bytes / 1024.0).roundToInt()} KB"
        bytes < 1024 * 1024 * 1024 -> "${(bytes / (1024.0 * 1024.0) * 10).roundToInt() / 10.0} MB"
        else -> "${(bytes / (1024.0 * 1024.0 * 1024.0) * 100).roundToInt() / 100.0} GB"
    }
}
