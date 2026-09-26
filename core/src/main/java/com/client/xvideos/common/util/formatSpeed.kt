package com.client.xvideos.common.util

import kotlin.math.roundToInt

private const val ONE_KB = 1024L
private const val ONE_MB = 1024L * 1024L
private const val ONE_GB = 1024L * 1024L * 1024L

private const val KB_DOUBLE = 1024.0
private const val MB_DOUBLE = 1024.0 * 1024.0
private const val GB_DOUBLE = 1024.0 * 1024.0 * 1024.0

fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0L) return "0 Bs"
    if (bytesPerSecond < ONE_KB) return "$bytesPerSecond Bs"
    return when {
        bytesPerSecond < ONE_MB -> "${(bytesPerSecond / KB_DOUBLE).roundToInt()} KBs"
        bytesPerSecond < ONE_GB -> "${(bytesPerSecond / MB_DOUBLE * 10).roundToInt() / 10.0} MBs"
        else -> "${(bytesPerSecond / GB_DOUBLE * 100).roundToInt() / 100.0} GBs"
    }
}

fun Long.formatAsSpeed(): String = formatSpeed(this)

fun Int.formatAsSpeed(): String = formatSpeed(this.toLong())

fun Float.formatAsSpeed(): String =
    formatSpeed(if (this.isNaN() || this.isInfinite() || this <= 0f) 0L else this.toLong())

fun Double.formatAsSpeed(): String =
    formatSpeed(if (this.isNaN() || this.isInfinite() || this <= 0.0) 0L else this.toLong())

fun formatSpeedOrDefault(bytesPerSecond: Long?, default: String = "0 Bs"): String =
    if (bytesPerSecond == null) default else formatSpeed(bytesPerSecond)

/** Расширение для форматирования скорости в битах в секунду (bps). */
fun Long.formatAsBitsPerSec(): String = "${this * 8L} bps"


