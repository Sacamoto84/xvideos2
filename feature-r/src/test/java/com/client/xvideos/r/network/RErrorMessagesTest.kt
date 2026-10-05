package com.client.xvideos.r.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Текст ошибки раздела R для экрана: без имени класса исключения и без адреса
 * сервера, которые несут сырые сообщения сети.
 */
class RErrorMessagesTest {

    @Test
    fun `нет сети — без адреса сервера в тексте`() {
        val message = UnknownHostException("Unable to resolve host \"api.example.com\"").toRUserMessage()

        assertEquals("Нет связи с сервером R", message)
        assertFalse(message.contains("example"))
    }

    @Test
    fun `таймаут — сервер не ответил вовремя`() {
        assertEquals("Сервер R не ответил вовремя", SocketTimeoutException("timeout").toRUserMessage())
    }

    @Test
    fun `обрыв соединения — нет связи`() {
        assertEquals("Нет связи с сервером R", IOException("Connection reset").toRUserMessage())
    }

    @Test
    fun `прочее — текст причины, а без него общее сообщение`() {
        assertEquals("ответ не разобран", IllegalStateException("ответ не разобран").toRUserMessage())
        assertEquals("Неизвестная ошибка R", IllegalStateException().toRUserMessage())
        assertEquals("Неизвестная ошибка R", (null as Throwable?).toRUserMessage())
    }
}
