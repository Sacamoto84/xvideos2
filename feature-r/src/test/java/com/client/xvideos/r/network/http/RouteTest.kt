package com.client.xvideos.r.network.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Сборка адреса запроса.
 *
 * Раньше кодирование делал самодельный `encodeURIComponent` из двадцати одного
 * `replace`. Проверок на него не было ни одной, а дыр было три: `%`, не-ASCII и
 * подстановка в собственный шаблон.
 *
 * **Чего эти тесты не проверяют.** Они идут на десктопной JVM, а на устройстве
 * регулярные выражения разбирает движок ICU, и он строже. Шаблон `\{(\w+)}` с
 * незакрытой фигурной скобкой JVM принимает, ICU — отвергает
 * `PatternSyntaxException`. Поймали это только на устройстве: приложение падало
 * на старте с `ExceptionInInitializerError`, потому что выражение лежит в
 * инициализаторе файла. Все тесты при этом были зелёные.
 *
 * Вывод на будущее: в регулярных выражениях экранировать всё, что можно
 * экранировать, даже если JVM не жалуется.
 */
class RouteTest {

    private fun urlOf(path: String, vararg params: Pair<String, Any>) =
        Route("GET", path, *params).url

    @Test
    fun `подстановка и база`() {
        assertEquals(
            "https://api.redgifs.com/v2/gifs/search?query=cat&page=1",
            urlOf("/v2/gifs/search?query={q}&page={page}", "q" to "cat", "page" to 1)
        )
    }

    /** `%` не экранировался вовсе, и сервер читал его как начало escape-последовательности. */
    @Test
    fun `процент экранируется`() {
        assertEquals(
            "https://api.redgifs.com/s?q=50%25",
            urlOf("/s?q={q}", "q" to "50%")
        )
    }

    /** Кириллица не трогалась никак — в адрес уходили сырые байты. */
    @Test
    fun `не-ASCII экранируется`() {
        val url = urlOf("/s?q={q}", "q" to "кот")
        assertEquals("https://api.redgifs.com/s?q=%D0%BA%D0%BE%D1%82", url)
    }

    /**
     * Главный случай. Значение первого параметра подставлялось раньше
     * остальных, поэтому текст поиска `{order}` попадал в результат и заменялся
     * следующей итерацией — подстановка в собственный шаблон.
     */
    @Test
    fun `значение не может подставиться в чужой шаблон`() {
        val url = urlOf(
            "/s?q={q}&order={order}",
            "q" to "{order}",
            "order" to "latest"
        )

        assertTrue(
            "порядок не должен был протечь в значение запроса: $url",
            !url.contains("q=latest")
        )
        assertTrue("сортировка на месте: $url", url.endsWith("&order=latest"))
    }

    @Test
    fun `пробел кодируется как %20, а не плюсом`() {
        assertEquals(
            "https://api.redgifs.com/s?q=big%20cat",
            urlOf("/s?q={q}", "q" to "big cat")
        )
    }

    @Test
    fun `служебные символы запроса экранируются`() {
        val url = urlOf("/s?q={q}", "q" to "a&b=c#d?e")
        assertEquals("https://api.redgifs.com/s?q=a%26b%3Dc%23d%3Fe", url)
    }

    /** Числа кодировать не в чем — подставляются как есть. */
    @Test
    fun `числа подставляются без кодирования`() {
        assertEquals(
            "https://api.redgifs.com/s?page=2&count=100",
            urlOf("/s?page={page}&count={count}", "page" to 2, "count" to 100)
        )
    }

    /**
     * Шаблон без значения остаётся нетронутым — так вело себя и прежнее
     * кодирование. Именно на этом попался мёртвый `searchCreators`, у которого
     * в пути стоял `count={count}` без параметра `count`.
     */
    @Test
    fun `шаблон без параметра остаётся как есть`() {
        assertEquals(
            "https://api.redgifs.com/s?count={count}",
            urlOf("/s?count={count}")
        )
    }

    @Test
    fun `путь без подстановок не меняется`() {
        assertEquals("https://api.redgifs.com/v1/tags", urlOf("/v1/tags"))
    }
}
