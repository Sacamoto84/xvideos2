package com.client.xvideos.common.webserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebServerStateTest {

    @Before
    fun setUp() {
        WebServerState.updateRunning(false)
        WebServerState.clearError()
    }

    @Test
    fun `updateRunning с true обновляет все поля и сбрасывает ошибку`() {
        WebServerState.setError("Старая ошибка")
        assertEquals("Старая ошибка", WebServerState.lastError.value)

        WebServerState.updateRunning(
            running = true,
            url = "http://192.168.1.100:8080",
            ip = "192.168.1.100",
            port = 8080,
            netName = "Home_WiFi"
        )

        assertTrue(WebServerState.isRunning.value)
        assertEquals("http://192.168.1.100:8080", WebServerState.serverUrl.value)
        assertEquals("192.168.1.100", WebServerState.ipAddress.value)
        assertEquals(8080, WebServerState.port.value)
        assertEquals("Home_WiFi", WebServerState.networkName.value)
        assertNull(WebServerState.lastError.value)
    }

    @Test
    fun `updateRunning с false сохраняет текст ошибки установленный через setError`() {
        WebServerState.setError("Порт 8080 уже занят другим приложением")
        WebServerState.updateRunning(running = false)

        assertFalse(WebServerState.isRunning.value)
        assertNull(WebServerState.serverUrl.value)
        assertEquals("Порт 8080 уже занят другим приложением", WebServerState.lastError.value)
    }

    @Test
    fun `clearError явно очищает ошибку`() {
        WebServerState.setError("Временная ошибка сети")
        assertEquals("Временная ошибка сети", WebServerState.lastError.value)

        WebServerState.clearError()
        assertNull(WebServerState.lastError.value)
    }
}
