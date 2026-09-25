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
fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length < 2) return "❓"
    val raw = if (countryCode.startsWith("flag-", ignoreCase = true)) countryCode.substring(5) else countryCode
    val code = raw.uppercase()
    if (code.length != 2 || code[0] !in 'A'..'Z' || code[1] !in 'A'..'Z') return "❓"
    val firstChar = code[0].code - 'A'.code + 0x1F1E6
    val secondChar = code[1].code - 'A'.code + 0x1F1E6
    val chars = CharArray(4)
    Character.toChars(firstChar, chars, 0)
    Character.toChars(secondChar, chars, 2)
    return String(chars)
}
