package com.client.xvideos.x.model

/**
 * Класс флага из разметки сайта (`flag-be`) — в эмодзи-флаг.
 *
 * Лежит в `model`, а не рядом с экраном выбора страны: этим пользуется и
 * парсер списка видео, то есть слой ниже UI. Зависимостей у функции нет,
 * поэтому нижний слой ей подходит.
 *
 * Некорректный код даёт «❓», а не исключение: разметка сайта меняется без
 * предупреждения, и ронять из-за этого разбор страницы незачем.
 */
const val UNKNOWN_FLAG = "❓"

fun isValidCountryCode(countryCode: String): Boolean = getFlagEmoji(countryCode) != UNKNOWN_FLAG

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length < 2) return UNKNOWN_FLAG
    val raw = if (countryCode.startsWith("flag-", ignoreCase = true)) countryCode.substring(5) else countryCode
    if (raw.length != 2) return UNKNOWN_FLAG
    val c0 = raw[0].uppercaseChar()
    val c1 = raw[1].uppercaseChar()
    if (c0 !in 'A'..'Z' || c1 !in 'A'..'Z') return UNKNOWN_FLAG
    val firstChar = c0.code - 'A'.code + 0x1F1E6
    val secondChar = c1.code - 'A'.code + 0x1F1E6
    val chars = CharArray(4)
    Character.toChars(firstChar, chars, 0)
    Character.toChars(secondChar, chars, 2)
    return String(chars)
}
