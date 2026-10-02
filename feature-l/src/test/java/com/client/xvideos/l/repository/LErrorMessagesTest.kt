package com.client.xvideos.l.repository

import com.client.xvideos.l.LServerErrorException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class LErrorMessagesTest {

    @Test
    fun `отказ сервера показывается как есть`() {
        assertEquals("Сервер L недоступен (HTTP 500)", LServerErrorException(500).toLUserMessage())
    }

    @Test
    fun `страница защиты вместо JSON не вываливает HTML пользователю`() {
        val error = IllegalStateException("Server returned HTML instead of JSON: <!DOCTYPE html><html>…")
        assertEquals("Сайт L ответил страницей защиты вместо данных, повторите позже", error.toLUserMessage())
    }

    @Test
    fun `сетевой сбой подписан как отсутствие связи`() {
        assertEquals("Нет связи с сервером L: Connection reset", IOException("Connection reset").toLUserMessage())
    }

    @Test
    fun `прочие ошибки — их текст, а без текста — общая фраза`() {
        assertEquals("boom", IllegalStateException("boom").toLUserMessage())
        assertEquals("Неизвестная ошибка L", IllegalStateException().toLUserMessage())
        assertEquals("Неизвестная ошибка L", null.toLUserMessage())
    }
}
