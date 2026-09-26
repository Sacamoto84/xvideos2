package com.client.xvideos.common.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private fun Long.absSafe(): Long = if (this == Long.MIN_VALUE) Long.MAX_VALUE else abs(this)

/**
 * 1_250   -> "1.2k"
 * 68_500  -> "68.5k"
 * 1_000_000 -> "1M"
 * 1_450_000 -> "1.4M"
 * 900     -> "900"
 */
fun Long.toPrettyCount(): String {
    if (this == 0L) return "0"
    val absValue = absSafe()

    return when {
        absValue < 1_000 -> absValue.toString()                     // 0-999

        absValue < 1_000_000 -> {                                   // 1.0k-999.9k
            val value = absValue / 1_000.0
            String.format(Locale.US, "%.1fK", value)
        }

        absValue < 1_000_000_000 -> {                               // 1.0M-999.9M
            val value = absValue / 1_000_000.0
            String.format(Locale.US, "%.1fM", value)
        }

        else -> {                                                   // 1.0B+
            val value = absValue / 1_000_000_000.0
            String.format(Locale.US, "%.1fB", value)
        }
    }
}

fun Long.toPrettyCount2(): String {
    if (this == 0L) return "0"
    val absValue = absSafe()

    return when {
        absValue < 1_000 -> absValue.toString()                     // 0-999

        absValue < 1_000_000 -> {                                   // 1.0k-999.9k
            val value = absValue / 1_000.0
            String.format(Locale.US, "%.2fK", value)
        }

        absValue < 1_000_000_000 -> {                               // 1.0M-999.9M
            val value = absValue / 1_000_000.0
            String.format(Locale.US, "%.2fM", value)
        }

        else -> {                                                   // 1.0B+
            val value = absValue / 1_000_000_000.0
            String.format(Locale.US, "%.2fB", value)
        }
    }
}

fun Long.toPrettyCount3(): String {
    if (this == 0L) return "0"
    val absValue = absSafe()

    return when {
        absValue < 1_000 -> absValue.toString()                     // 0-999

        absValue < 1_000_000 -> {                                   // 1.0k-999.9k
            val value = absValue / 1_000.0
            String.format(Locale.US, "%.3fK", value)
        }

        absValue < 1_000_000_000 -> {                               // 1.0M-999.9M
            val value = absValue / 1_000_000.0
            String.format(Locale.US, "%.3fM", value)
        }

        else -> {                                                   // 1.0B+
            val value = absValue / 1_000_000_000.0
            String.format(Locale.US, "%.3fB", value)
        }
    }
}

fun Long.toPrettyCountInt(): String {
    if (this == 0L) return "0"
    val absValue = absSafe()

    return when {
        absValue < 1_000 -> absValue.toString()                     // 0-999
        absValue < 1_000_000 -> "${(absValue / 1_000.0).roundToLong()}K"
        absValue < 1_000_000_000 -> "${(absValue / 1_000_000.0).roundToLong()}M"
        else -> "${(absValue / 1_000_000_000.0).roundToLong()}B"
    }
}

fun Int.toPrettyCount(): String = this.toLong().toPrettyCount()

fun Int.toPrettyCount2(): String = this.toLong().toPrettyCount2()

fun Int.toPrettyCount3(): String = this.toLong().toPrettyCount3()

fun Int.toPrettyCountInt(): String = this.toLong().toPrettyCountInt()

fun String?.toPrettyCountOrDefault(default: String = "0"): String =
    this?.toLongOrNull()?.toPrettyCount() ?: default

