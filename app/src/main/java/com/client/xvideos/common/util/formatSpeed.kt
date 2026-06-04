package com.client.xvideos.common.util

import kotlin.math.roundToInt

fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 0 -> "0 Bs"
        bytesPerSecond < 1024 -> "$bytesPerSecond Bs"
        bytesPerSecond < 1024 * 1024 -> "${(bytesPerSecond / 1024.0).roundToInt()} KBs"
        bytesPerSecond < 1024 * 1024 * 1024 -> "${(bytesPerSecond / (1024.0 * 1024.0) * 10).roundToInt() / 10.0} MBs"
        else -> "${(bytesPerSecond / (1024.0 * 1024.0 * 1024.0) * 100).roundToInt() / 100.0} GBs"
    }
}
