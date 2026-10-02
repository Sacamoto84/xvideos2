package com.client.xvideos.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LWebProbeTest {

    @Test
    fun `анонимный GraphQL-запрос описывается операцией и переменными без хоста и текста запроса`() {
        val url = "https://www.site.test/graphql/nobatch/?operationName=AlbumList" +
            "&query=query%2520AlbumList%2520%257B%2520a%2520%257D&variables=%7B%22limit%22%3A12%2C%22s%22%3A%22a+b%22%7D"

        assertEquals("""op=AlbumList vars={"limit":12,"s":"a b"}""", describeGraphQlRequest(url))
    }

    @Test
    fun `не GraphQL — не описывается`() {
        assertNull(describeGraphQlRequest("https://www.site.test/albums/new/"))
        assertNull(describeGraphQlRequest("https://static.site.test/build/client/main.js"))
    }

    @Test
    fun `пропускаются домен сайта, его поддомены и проверка Cloudflare, остальное режется`() {
        assertTrue(isAllowedHost("www.site.test", "site.test"))
        assertTrue(isAllowedHost("static.site.test", "site.test"))
        assertTrue(isAllowedHost("site.test", "site.test"))
        assertTrue(isAllowedHost("challenges.cloudflare.com", "site.test"))
        assertFalse(isAllowedHost("a.adprovider.example", "site.test"))
        assertFalse(isAllowedHost("evilsite.test", "site.test"))
    }

    @Test
    fun `строка выгрузки хранит операцию, раскодированный текст запроса, переменные и сырую строку параметров`() {
        val rawQuery = "operationName=Q&query=%2520query%2520Q%2520%257B%2520a%28b%253A%2520%2522x%252B1%2522%29%2520%257D%2520" +
            "&variables=%7B%22p%22%3A1%7D"

        val line = siteRequestDumpLine("https://www.site.test/graphql/nobatch/?$rawQuery")

        assertEquals(
            """{"op":"Q","query":" query Q { a(b: \"x+1\") } ","variables":"{\"p\":1}","rawQuery":"$rawQuery"}""",
            line
        )
    }

    @Test
    fun `не GraphQL — строки выгрузки нет`() {
        assertNull(siteRequestDumpLine("https://www.site.test/manga/"))
    }

    @Test
    fun `альбомы считаются по typename`() {
        assertEquals(2, countAlbums("""{"a":{"__typename":"Album"},"b":{"__typename" : "Album"},"c":{"__typename":"AlbumTag"}}"""))
    }
}
