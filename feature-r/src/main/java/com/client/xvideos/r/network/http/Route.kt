package com.client.xvideos.r.network.http

import io.ktor.http.encodeURLParameter

/**
 * `{имя}` в шаблоне пути.
 *
 * Закрывающая скобка экранирована, и это не косметика. JVM на десктопе принимает
 * `\{(\w+)}`, а движок регулярных выражений Android (ICU) отвергает его с
 * `PatternSyntaxException: Syntax error in regexp pattern near index 8`. Так как
 * выражение лежит в инициализаторе файла, падал не запрос, а весь класс —
 * `ExceptionInInitializerError` на старте приложения, при первом же обращении к
 * API из `SavedRed.refreshTagList`.
 *
 * Юнит-тесты этого не ловят: они идут на десктопной JVM с другим движком.
 */
private val PLACEHOLDER = Regex("""\{(\w+)\}""")

/**
 * Адрес запроса: шаблон пути плюс значения подстановок.
 *
 * ```
 * Route("GET", "/v2/gifs/search?query={q}&page={page}", "q" to "cat", "page" to 1)
 * ```
 *
 * Кодирование отдано ktor'у ([encodeURLParameter]). Раньше здесь лежал
 * самодельный `encodeURIComponent` из двадцати одного `replace`, и он не
 * закрывал три случая:
 *
 * - **`%`** не экранировался вовсе. Запрос «50%» уходил как есть, и сервер
 *   читал `%` как начало escape-последовательности;
 * - **не-ASCII** — кириллица и всё прочее — не трогался никак;
 * - **`{` и `}`** не экранировались, а подстановка шла последовательными
 *   `replace` по всей строке. Значение первого параметра подставлялось раньше
 *   остальных, поэтому текст поиска `{order}` попадал в результат и заменялся
 *   следующей итерацией — подстановка в собственный шаблон.
 *
 * Третий случай закрыт заодно с первыми двумя: [PLACEHOLDER] проходит строку
 * один раз, и подставленное значение повторно не осматривается.
 */
class Route(val method: String, val path: String, vararg parameters: Pair<String, Any>) {

    val url: String = BASE + path.fillPlaceholders(parameters.toMap())

    private fun String.fillPlaceholders(params: Map<String, Any>): String =
        PLACEHOLDER.replace(this) { match ->
            // Нет такого параметра — оставляем шаблон нетронутым: так вели себя
            // и прежние replace. Молча подставлять пустоту хуже — неверный
            // адрес виден в логе, пустой параметр незаметен.
            val value = params[match.groupValues[1]] ?: return@replace match.value
            when (value) {
                // Числа и флаги кодировать не в чем; строки — всегда.
                is String -> value.encodeURLParameter()
                else -> value.toString()
            }
        }

    private companion object {
        const val BASE = "https://api.redgifs.com"
    }
}
