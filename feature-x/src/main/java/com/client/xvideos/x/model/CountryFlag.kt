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
    val code = countryCode.replace("flag-", "").uppercase()
    if (code.length != 2) return "❓"
    val firstChar = code[0].code - 'A'.code + 0x1F1E6
    val secondChar = code[1].code - 'A'.code + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}
