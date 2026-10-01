package com.client.xvideos.x.feature.net

import org.junit.Assert.assertEquals
import org.junit.Test

class DecodeJsStringResultTest {

    @Test
    fun `разбирает все экранирования JSON-строки, а не только угловые скобки`() {
        // Так evaluateJavascript отдаёт строку: в кавычках, с \\, \t, \/ и \uXXXX.
        val raw = "\"<p>a\\\\b\\tc \\u003Cd\\u003E https:\\/\\/x.test\\/v?a=1\\u0026b=2 \\\"q\\\"</p>\""

        assertEquals(
            "<p>a\\b\tc <d> https://x.test/v?a=1&b=2 \"q\"</p>",
            decodeJsStringResult(raw)
        )
    }

    @Test
    fun `пустой и null результат дают пустую строку`() {
        assertEquals("", decodeJsStringResult(null))
        assertEquals("", decodeJsStringResult(""))
        assertEquals("", decodeJsStringResult("null"))
    }

    @Test
    fun `не строковый JSON не разбирается и даёт пустую строку`() {
        assertEquals("", decodeJsStringResult("{\"a\":1}"))
        assertEquals("", decodeJsStringResult("\"незакрытая строка"))
    }
}
