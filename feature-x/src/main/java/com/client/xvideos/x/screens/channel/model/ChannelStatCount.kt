package com.client.xvideos.x.screens.channel.model

import com.client.xvideos.common.util.toPrettyCount
import kotlin.math.roundToLong

private val THOUSANDS_WITH_DOTS = Regex("""^\d{1,3}(\.\d{3})+$""")

/**
 * Счётчик шапки профиля в коротком виде, как в R: одна цифра после точки
 * (`720.8M`, `715.9K`).
 *
 * Сайт отдаёт просмотры полным числом с разделителем разрядов
 * (`720 776 005`), а подписчиков — уже сокращёнными по-своему (`715,9 к`,
 * `1,5 М`). Оба вида приводятся к одному. Текст, который не удалось разобрать,
 * показывается как есть.
 */
fun channelStatCount(text: String): String = parseStatCount(text)?.toPrettyCount() ?: text.trim()

private fun parseStatCount(text: String): Long? {
    val compact = text.filterNot { it.isWhitespace() }.lowercase()
    if (compact.isEmpty()) return null
    val multiplier = when (compact.last()) {
        'k', 'к' -> 1_000.0
        'm', 'м' -> 1_000_000.0
        'b' -> 1_000_000_000.0
        else -> null
    }
    if (multiplier != null) {
        val number = compact.dropLast(1).replace(',', '.').toDoubleOrNull() ?: return null
        return (number * multiplier).roundToLong()
    }
    val noCommas = compact.replace(",", "")
    val digits = if (THOUSANDS_WITH_DOTS.matches(noCommas)) noCommas.replace(".", "") else noCommas
    return digits.toLongOrNull()
}
