package com.client.xvideos.x.parcer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Число роликов в шапке канала. От него считается число страниц, а сайт пишет
 * большие числа с разделителем разрядов — такое число раньше превращалось в 0.
 */
class ParserChannelVideoCountTest {

    private fun videoCount(text: String): Int {
        val html = """<html><body><div id="tab-videos"><span class="count">$text</span></div></body></html>"""
        return parserChannelHeader(html, fallbackSlug = "channel").videoCount
    }

    @Test
    fun `число без разделителей разбирается как раньше`() {
        assertEquals(70, videoCount("70"))
    }

    @Test
    fun `пробел между разрядами не мешает`() {
        assertEquals(1234, videoCount("1 234"))
    }

    @Test
    fun `неразрывный пробел между разрядами не мешает`() {
        assertEquals(12345, videoCount("12 345"))
    }

    @Test
    fun `запятая между разрядами не мешает`() {
        assertEquals(1234567, videoCount("1,234,567"))
    }

    @Test
    fun `точка между тройками разрядов не мешает`() {
        assertEquals(1234, videoCount("1.234"))
    }

    @Test
    fun `сокращённое число не угадывается`() {
        assertEquals(0, videoCount("1.2k"))
    }

    @Test
    fun `пустой счётчик даёт ноль`() {
        assertEquals(0, videoCount(""))
    }
}
