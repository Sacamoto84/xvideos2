package com.client.xvideos.common.util

import java.util.Locale

/**
 * Возвращает строку, в которой первая буква каждого слова преобразована в верхний регистр,
 * а остальные буквы — в нижний.
 *
 * Пример:
 * ```
 * "hello world".capitalizeEachWord() // "Hello World"
 * "мИр ПрИвЕт".capitalizeEachWord() // "Мир Привет"
 * ```
 *
 * Разделителем слов считается пробел (`' '`), знаки препинания и спецсимволы не обрабатываются.
 *
 * @return Новая строка, в которой каждое слово начинается с заглавной буквы.
 */
fun String.capitalizeEachWord(): String {
    if (isEmpty()) return ""
    if (length == 1) return uppercase(Locale.getDefault())
    if (isBlank()) return this
    val lower = lowercase(Locale.getDefault())
    val sb = StringBuilder(lower.length)
    var capitalizeNext = true
    for (i in 0 until lower.length) {
        val c = lower[i]
        if (c == ' ') {
            sb.append(' ')
            capitalizeNext = true
        } else if (capitalizeNext) {
            sb.append(c.uppercaseChar())
            capitalizeNext = false
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}
