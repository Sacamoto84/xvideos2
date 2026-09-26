package com.client.xvideos.x.search

import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParseJsonTest {

    @Test
    fun `parseJson возвращает null для пустой или пробельной строки`() {
        assertNull(parseJson(null))
        assertNull(parseJson(""))
        assertNull(parseJson("   "))
        assertNull(parseJson("\t\n"))
    }

    @Test
    fun `parseJson возвращает null для некорректного JSON`() {
        assertNull(parseJson("{ not a json }"))
        assertNull(parseJson("404 Not Found"))
    }

    @Test
    fun `parseJson успешно разбирает валидный ответ с игнорированием лишних полей`() {
        val json = """
            {
                "result": true,
                "code": 200,
                "keywords": [
                    {"N": "asian", "R": "82.887"}
                ],
                "unknownField": "should be ignored"
            }
        """.trimIndent()

        val parsed = parseJson(json)
        assertNotNull(parsed)
        assertEquals(true, parsed?.result)
        assertEquals(200, parsed?.code)
        assertEquals(1, parsed?.keywords?.size)
        assertEquals("asian", parsed?.keywords?.first()?.N)
        assertEquals("82.887", parsed?.keywords?.first()?.R)
    }

    @Test
    fun `parseJson корректно разбирает pornstar и channel при отсутствии и наличии поля A`() {
        val json = """
            {
                "result": true,
                "code": 200,
                "keywords": [],
                "pornstar": [
                    {
                        "N": "Eva Elfie",
                        "F": "/profiles/eva-elfie",
                        "T": "pornstar",
                        "MV": 100,
                        "M": 10,
                        "L": 5,
                        "P": "https://img.xv-ru.com/eva.jpg",
                        "RF": "500K"
                    }
                ],
                "channel": [
                    {
                        "N": "Brazzers",
                        "F": "/profiles/brazzers",
                        "T": "channel",
                        "CPV": true,
                        "M": 0,
                        "L": 0,
                        "P": "https://img.xv-ru.com/brazzers.jpg",
                        "RF": "1M",
                        "A": {"verified": "1"}
                    }
                ],
                "BLACKLISTED": false
            }
        """.trimIndent()

        val parsed = parseJson(json)
        assertNotNull(parsed)
        assertEquals(1, parsed?.pornstar?.size)
        assertEquals(null, parsed?.pornstar?.first()?.A)
        assertEquals(1, parsed?.channel?.size)
        assertEquals(mapOf("verified" to "1"), parsed?.channel?.first()?.A)
        assertEquals(false, parsed?.BLACKLISTED)
    }

    @Test
    fun `Keyword, Pornstar, Channel, and SearchResult helpers operate correctly`() {
        val emptyKeyword = Keyword.EMPTY
        assertEquals(false, emptyKeyword.isValid)
        assertEquals("", emptyKeyword.name)
        assertEquals("", emptyKeyword.rating)

        val validKeyword = Keyword(N = "blonde", R = "95.5")
        assertEquals(true, validKeyword.isValid)
        assertEquals("blonde", validKeyword.name)
        assertEquals("95.5", validKeyword.rating)

        val emptyPornstar = Pornstar.EMPTY
        assertEquals(false, emptyPornstar.isValid)
        assertEquals("", emptyPornstar.name)
        assertEquals(0, emptyPornstar.videoCount)

        val validPornstar = Pornstar(
            N = "Star",
            F = "/profiles/star",
            T = "pornstar",
            MV = 15,
            M = 1,
            L = 2,
            P = "https://example/p.jpg",
            RF = "10K"
        )
        assertEquals(true, validPornstar.isValid)
        assertEquals("Star", validPornstar.name)
        assertEquals("/profiles/star", validPornstar.profilePath)
        assertEquals("https://example/p.jpg", validPornstar.avatarUrl)
        assertEquals(15, validPornstar.videoCount)
        assertEquals("10K", validPornstar.subscribers)

        val emptyChannel = Channel.EMPTY
        assertEquals(false, emptyChannel.isValid)
        assertEquals("", emptyChannel.name)

        val validChannel = Channel(
            N = "Studio",
            F = "/profiles/studio",
            T = "channel",
            CPV = true,
            M = 0,
            L = 0,
            P = "https://example/ch.jpg",
            RF = "25K"
        )
        assertEquals(true, validChannel.isValid)
        assertEquals("Studio", validChannel.name)
        assertEquals("/profiles/studio", validChannel.profilePath)
        assertEquals("https://example/ch.jpg", validChannel.avatarUrl)
        assertEquals("25K", validChannel.subscribers)

        val emptyResult = SearchResult.EMPTY
        assertEquals(true, emptyResult.isEmpty)
        assertEquals(false, emptyResult.isNotEmpty)
        assertEquals(false, emptyResult.hasKeywords)
        assertEquals(false, emptyResult.hasPornstars)
        assertEquals(false, emptyResult.hasChannels)
        assertEquals(false, emptyResult.isBlacklisted)

        val populatedResult = SearchResult(
            result = true,
            code = 200,
            keywords = listOf(validKeyword),
            pornstar = listOf(validPornstar),
            channel = listOf(validChannel),
            BLACKLISTED = true
        )
        assertEquals(false, populatedResult.isEmpty)
        assertEquals(true, populatedResult.isNotEmpty)
        assertEquals(true, populatedResult.hasKeywords)
        assertEquals(true, populatedResult.hasPornstars)
        assertEquals(true, populatedResult.hasChannels)
        assertEquals(true, populatedResult.isBlacklisted)
    }
}

