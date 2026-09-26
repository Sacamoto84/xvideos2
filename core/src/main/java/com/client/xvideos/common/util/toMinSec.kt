package com.client.xvideos.common.util

private val DIGIT_STRINGS = Array(60) { if (it < 10) "0$it" else it.toString() }

/**
 *  68.7   → "01:08"
 * 134.0   → "02:14"
 *  9.2    → "00:09"
 */
fun Double.toMinSec(): String {
    if (this.isNaN() || this.isInfinite() || this <= 0.0) return "00:00"
    val totalSec = if (this > Int.MAX_VALUE) Int.MAX_VALUE else this.toInt()
    if (totalSec <= 0) return "00:00"
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val mStr = if (minutes < 60) DIGIT_STRINGS[minutes] else minutes.toString()
    val sStr = if (seconds < 60) DIGIT_STRINGS[seconds] else seconds.toString()
    return "$mStr:$sStr"
}

fun Float.toMinSec(): String {
    if (this.isNaN() || this.isInfinite() || this <= 0f) return "00:00"
    val totalSec = if (this > Int.MAX_VALUE) Int.MAX_VALUE else this.toInt()
    if (totalSec <= 0) return "00:00"
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val mStr = if (minutes < 60) DIGIT_STRINGS[minutes] else minutes.toString()
    val sStr = if (seconds < 60) DIGIT_STRINGS[seconds] else seconds.toString()
    return "$mStr:$sStr"
}

fun Int.toMinSec(): String {
    if (this <= 0) return "00:00"
    val minutes = this / 60
    val seconds = this % 60
    val mStr = if (minutes < 60) DIGIT_STRINGS[minutes] else minutes.toString()
    val sStr = if (seconds < 60) DIGIT_STRINGS[seconds] else seconds.toString()
    return "$mStr:$sStr"
}

fun Long.toMinSec(): String {
    if (this <= 0L) return "00:00"
    val totalSec = if (this > Int.MAX_VALUE) Int.MAX_VALUE else this.toInt()
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val mStr = if (minutes < 60) DIGIT_STRINGS[minutes] else minutes.toString()
    val sStr = if (seconds < 60) DIGIT_STRINGS[seconds] else seconds.toString()
    return "$mStr:$sStr"
}

