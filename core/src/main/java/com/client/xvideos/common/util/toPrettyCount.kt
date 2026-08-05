package com.client.xvideos.common.util

import android.annotation.SuppressLint
import java.util.Locale
import kotlin.math.abs

/**
 * 1_250   -> "1.2k"
 * 68_500  -> "68.5k"
 * 1_000_000 -> "1M"
 * 1_450_000 -> "1.4M"
 * 900     -> "900"
 */
@SuppressLint("DefaultLocale")
fun Long.toPrettyCount(): String {
    val absValue = abs(this)

    return when {
        absValue < 1_000 -> "$absValue"                             // 0-999

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

@SuppressLint("DefaultLocale")
fun Long.toPrettyCount2(): String {
    val absValue = abs(this)

    return when {
        absValue < 1_000 -> "$absValue"                             // 0-999

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

@SuppressLint("DefaultLocale")
fun Long.toPrettyCount3(): String {
    val absValue = abs(this)

    return when {
        absValue < 1_000 -> "$absValue"                             // 0-999

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

@SuppressLint("DefaultLocale")
fun Long.toPrettyCountInt(): String {
    val absValue = abs(this)

    return when {
        absValue < 1_000 -> "$absValue"                             // 0-999

        absValue < 1_000_000 -> {                                   // 1.0k-999.9k
            val value = absValue / 1_000.0
            String.format(Locale.US, "%.0fK", value)
        }

        absValue < 1_000_000_000 -> {                               // 1.0M-999.9M
            val value = absValue / 1_000_000.0
            String.format(Locale.US, "%.0fM", value)
        }

        else -> {                                                   // 1.0B+
            val value = absValue / 1_000_000_000.0
            String.format(Locale.US, "%.0fB", value)
        }
    }
}
