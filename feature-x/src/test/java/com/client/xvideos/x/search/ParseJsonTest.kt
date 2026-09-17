package com.client.xvideos.x.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ParseJsonTest {

    @Test
    fun `parseJson возвращает null для пустой или пробельной строки`() {
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
}

