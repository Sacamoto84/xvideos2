package com.client.xvideos.common.webserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Порт веб-сервера из поля ввода.
 *
 * Порт хранился в настройках, но сменить его было нечем: при занятом 8080
 * сервер не запускался, и поправить это из приложения было нельзя.
 */
class WebServerPortTest {

    @Test
    fun `число в допустимых пределах принимается, пробелы по краям не мешают`() {
        assertEquals(8080, parseWebServerPort("8080").getOrThrow())
        assertEquals(1024, parseWebServerPort(" 1024 ").getOrThrow())
        assertEquals(65535, parseWebServerPort("65535").getOrThrow())
    }

    @Test
    fun `системные порты и числа вне диапазона отклоняются`() {
        assertTrue(parseWebServerPort("80").isFailure)
        assertTrue(parseWebServerPort("1023").isFailure)
        assertTrue(parseWebServerPort("65536").isFailure)
        assertTrue(parseWebServerPort("-8080").isFailure)
    }

    @Test
    fun `не число отклоняется`() {
        assertTrue(parseWebServerPort("").isFailure)
        assertTrue(parseWebServerPort("порт").isFailure)
        assertTrue(parseWebServerPort("80 80").isFailure)
    }
}
