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

/**
 * Проверяет, поддерживается ли двухбуквенный код страны и формирует ли он валидный флаг-эмодзи.
 */
fun isValidCountryCode(countryCode: String): Boolean = getFlagEmoji(countryCode) != UNKNOWN_FLAG

/**
 * Преобразует двухбуквенный код страны (или CSS-класс вида `flag-xx`) в соответствующий флаг-эмодзи Юникода.
 *
 * @param countryCode Двухбуквенный код страны (например, `"us"`, `"ru"`, `"flag-fr"`).
 * @return Эмодзи флага страны либо [UNKNOWN_FLAG] при некорректном коде.
 */
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

/**
 * Преобразует код страны в эмодзи флага либо возвращает `null`, если код не распознан.
 */
fun getFlagEmojiOrNull(countryCode: String?): String? {
    if (countryCode == null) return null
    val emoji = getFlagEmoji(countryCode)
    return if (emoji != UNKNOWN_FLAG) emoji else null
}

/**
 * Преобразует код страны в эмодзи флага либо возвращает [default], если код не распознан.
 */
fun getFlagEmojiOrDefault(countryCode: String?, default: String = UNKNOWN_FLAG): String =
    getFlagEmojiOrNull(countryCode) ?: default

/**
 * Extension-свойство/функция для преобразования nullable строки в флаг-эмодзи.
 */
fun String?.toCountryFlagEmoji(): String =
    if (this == null) UNKNOWN_FLAG else getFlagEmoji(this)

