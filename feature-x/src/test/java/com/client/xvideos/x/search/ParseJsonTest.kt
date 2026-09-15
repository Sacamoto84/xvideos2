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
}
