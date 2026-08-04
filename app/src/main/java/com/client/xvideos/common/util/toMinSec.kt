package com.client.xvideos.common.util

/**
 *  68.7   → "01:08"
 * 134.0   → "02:14"
 *  9.2    → "00:09"
 */
fun Double.toMinSec(): String {
    val totalSec = this.toInt()                     // отбрасываем дробную часть
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%02d:%02d".format(minutes, seconds)     // ведущие нули
}
