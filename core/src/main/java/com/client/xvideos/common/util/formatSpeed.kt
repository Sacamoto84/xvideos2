package com.client.xvideos.common.util

import kotlin.math.roundToInt

fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond <= 0L -> "0 Bs"
        bytesPerSecond < 1024L -> "$bytesPerSecond Bs"
        bytesPerSecond < 1024L * 1024L -> "${(bytesPerSecond / 1024.0).roundToInt()} KBs"
        bytesPerSecond < 1024L * 1024L * 1024L -> "${(bytesPerSecond / (1024.0 * 1024.0) * 10).roundToInt() / 10.0} MBs"
        else -> "${(bytesPerSecond / (1024.0 * 1024.0 * 1024.0) * 100).roundToInt() / 100.0} GBs"
    }
}
