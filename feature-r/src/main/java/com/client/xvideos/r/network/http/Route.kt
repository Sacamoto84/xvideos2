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
 * Маршрут и параметры HTTP-запроса к API RedGifs.
 *
 * Инкапсулирует HTTP-метод, шаблон пути с именованными плейсхолдерами вида `{param}`
 * и вариативный список аргументов подстановки.
 *
 * Пример использования:
 * ```kotlin
 * Route("GET", "/v2/gifs/search?query={q}&page={page}", "q" to "cat", "page" to 1)
 * ```
 *
 * Кодирование параметров делегировано Ktor ([encodeURLParameter]).
 * Защищает от трёх типичных дефектов наивной подстановки:
 * 1. Символ `%` теперь корректно URL-экранируется.
 * 2. Символы вне ASCII (кириллица и др.) безопасно кодируются.
 * 3. Фигурные скобки `{` и `}` в значениях параметров экранируются и не приводят
 *    к каскадной подстановке в собственный шаблон (однопроходный [PLACEHOLDER]).
 *
 * @property method HTTP-метод (например, "GET", "POST").
 * @property path Относительный шаблон URL с плейсхолдерами в фигурных скобках.
 * @param parameters Список пар (ключ, значение) для заполнения плейсхолдеров в [path].
 */
class Route(val method: String, val path: String, vararg parameters: Pair<String, Any>) {

    /** Флаг наличия переданных параметров подстановки. */
    private val hasVarargParams: Boolean = parameters.isNotEmpty()

    /**
     * Итоговый абсолютный URL-адрес запроса с префиксом [BASE] и подставленными параметрами.
     * Если [path] пуст, возвращает пустую строку.
     */
    val url: String = when {
        path.isEmpty() -> ""
        parameters.isEmpty() || !path.contains('{') -> BASE + path
        else -> BASE + path.fillPlaceholders(parameters)
    }

    /** Проверяет, является ли HTTP-метод GET (без учета регистра). */
    val isGet: Boolean get() = method.equals("GET", ignoreCase = true)

    /** Проверяет, является ли HTTP-метод POST (без учета регистра). */
    val isPost: Boolean get() = method.equals("POST", ignoreCase = true)

    /**
     * Показывает, содержит ли маршрут параметры (переданные в vararg либо
     * присутствующие как query-параметры `?` в итоговом URL).
     */
    val hasParameters: Boolean get() = hasVarargParams || url.contains('?')

    /** Проверяет, относится ли маршрут к API v1. */
    val isApiV1: Boolean get() = path.startsWith("/v1/")

    /** Проверяет, относится ли маршрут к API v2. */
    val isApiV2: Boolean get() = path.startsWith("/v2/")

    /** Относительный путь без query-параметров. */
    val pathWithoutQuery: String get() = path.substringBefore('?')

    /** Проверяет соответствие пути заданному пути (с учетом или без query-параметров). */
    fun matchesPath(expectedPath: String): Boolean =
        pathWithoutQuery == expectedPath || path == expectedPath

    override fun toString(): String = url

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Route) return false
        return method == other.method && url == other.url
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }

    /**
     * Выполняет однопроходную замену плейсхолдеров `{param}` в строке шаблона.
     * Ненайденные параметры остаются в шаблоне без изменений для упрощения отладки в логах.
     */
    private fun String.fillPlaceholders(params: Array<out Pair<String, Any>>): String =
        PLACEHOLDER.replace(this) { match ->
            // Нет такого параметра — оставляем шаблон нетронутым: так вели себя
            // и прежние replace. Молча подставлять пустоту хуже — неверный
            // адрес виден в логе, пустой параметр незаметен.
            val key = match.groupValues[1]
            val value = params.firstOrNull { it.first == key }?.second ?: return@replace match.value
            when (value) {
                // Числа и флаги кодировать не в чем; строки — всегда.
                is String -> value.encodeURLParameter()
                else -> value.toString()
            }
        }

    companion object {
        /** Базовый хост API RedGifs. */
        const val BASE = "https://api.redgifs.com"

        /** Создает GET-маршрут с параметрами подстановки. */
        fun get(path: String, vararg parameters: Pair<String, Any>): Route =
            Route("GET", path, *parameters)

        /** Создает POST-маршрут с параметрами подстановки. */
        fun post(path: String, vararg parameters: Pair<String, Any>): Route =
            Route("POST", path, *parameters)
    }
}
