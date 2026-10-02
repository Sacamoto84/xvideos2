package com.client.xvideos.l

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Анонимный GET-запрос GraphQL обязан совпадать с запросом сайта байт в байт:
 * ключ кэша Cloudflare — полный URL, и только при совпадении ответ берётся из
 * кэша. Ожидаемые строки посчитаны вручную по правилам браузера:
 * `encodeURIComponent` для текста запроса, затем `URLSearchParams` для всех
 * параметров (поэтому текст запроса закодирован дважды).
 */
class AnonymousGraphQlUrlTest {

    private val endpoint = "https://h/graphql/nobatch/"

    @Test
    fun `запрос кодируется как на сайте — пробелы схлопнуты, текст закодирован дважды`() {
        val data = """{"operationName":"Q","query":"\n  query Q {\n    a(b: \"x y\")\n  }\n","variables":{"p":1}}"""

        val url = anonymousGraphQlGetUrl(endpoint, data)

        assertEquals(
            "https://h/graphql/nobatch/?operationName=Q" +
                "&query=%2520query%2520Q%2520%257B%2520a%28b%253A%2520%2522x%2520y%2522%29%2520%257D%2520" +
                "&variables=%7B%22p%22%3A1%7D",
            url
        )
    }

    @Test
    fun `переменные сериализуются компактно, тильда кодируется, звёздочка нет`() {
        val data = """{"operationName":"Q","query":"query Q { a }","variables": { "s" : "a~b*c" }}"""

        val url = anonymousGraphQlGetUrl(endpoint, data)

        assertEquals(
            "https://h/graphql/nobatch/?operationName=Q&query=query%2520Q%2520%257B%2520a%2520%257D" +
                "&variables=%7B%22s%22%3A%22a%7Eb*c%22%7D",
            url
        )
    }

    @Test
    fun `мутация не уходит в GET`() {
        val data = """{"operationName":"FavoriteAdd","query":"mutation FavoriteAdd { x }","variables":{}}"""
        assertNull(anonymousGraphQlGetUrl(endpoint, data))
    }

    @Test
    fun `слишком длинный запрос не уходит в GET`() {
        val longField = "a".repeat(9000)
        val data = """{"operationName":"Q","query":"query Q { $longField }","variables":{}}"""
        assertNull(anonymousGraphQlGetUrl(endpoint, data))
    }

    @Test
    fun `нераспознанное тело не уходит в GET`() {
        assertNull(anonymousGraphQlGetUrl(endpoint, "not json"))
        assertNull(anonymousGraphQlGetUrl(endpoint, """{"operationName":"Q"}"""))
    }
}
