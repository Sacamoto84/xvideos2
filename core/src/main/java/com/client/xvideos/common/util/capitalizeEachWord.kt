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
fun String.capitalizeEachWord(): String =
    lowercase(Locale.getDefault())
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
