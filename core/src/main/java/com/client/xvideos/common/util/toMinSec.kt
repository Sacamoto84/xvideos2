package com.client.xvideos.common.util

/**
 *  68.7   → "01:08"
 * 134.0   → "02:14"
 *  9.2    → "00:09"
 */
fun Double.toMinSec(): String {
    if (this.isNaN() || this <= 0.0) return "00:00"
    val totalSec = if (this > Int.MAX_VALUE) Int.MAX_VALUE else this.toInt()
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%02d:%02d".format(minutes, seconds)     // ведущие нули
}

fun Float.toMinSec(): String = this.toDouble().toMinSec()
