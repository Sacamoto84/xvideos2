package com.client.xvideos.common.coil

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Карта прогресса картинок держит только идущие загрузки (проход 206, T1).
 * Раньше записи не удалял никто, и карта росла на каждую картинку из сети.
 */
class CoilProgressTrackingTest {

    private val url = "https://example.com/pic.jpg"

    @After
    fun tearDown() {
        CoilProgressManager.clearAll()
    }

    @Test
    fun `идущая загрузка видна в карте`() {
        CoilProgressManager.updateProgress(url, bytes = 512, total = 2048)

        val item = CoilProgressManager.progressMap[url]
        assertEquals(512L, item?.bytes)
        assertEquals(2048L, item?.total)
    }

    @Test
    fun `законченная загрузка из карты убирается`() {
        CoilProgressManager.updateProgress(url, bytes = 512, total = 2048)
        CoilProgressManager.updateProgress(url, bytes = 2048, total = 2048, done = true)

        assertNull(CoilProgressManager.progressMap[url])
        assertTrue(CoilProgressManager.progressMap.isEmpty())
    }

    @Test
    fun `дочитанное тело сообщает о конце загрузки`() {
        val events = mutableListOf<Boolean>()
        val body = ProgressResponseBody("hello world".toResponseBody("text/plain".toMediaType())) { _, _, done ->
            events += done
        }

        body.string()

        assertTrue(events.last())
    }

    @Test
    fun `закрытое до конца тело тоже сообщает о конце загрузки`() {
        // Отменённая загрузка: Coil закрывает ответ, не дочитав его, и read()
        // до -1 не доходит. Без сигнала при закрытии запись осталась бы навсегда.
        val events = mutableListOf<Boolean>()
        val body = ProgressResponseBody(ByteArray(64 * 1024).toResponseBody()) { _, _, done ->
            events += done
        }

        body.source().read(okio.Buffer(), 1024)
        assertFalse(events.last())

        body.close()

        assertTrue(events.last())
    }

    @Test
    fun `через перехватчик после загрузки в карте не остаётся записи`() {
        val body = ProgressResponseBody("payload".toResponseBody()) { bytes, total, done ->
            CoilProgressManager.updateProgress(url, bytes, total, done)
        }

        body.string()

        assertNull(CoilProgressManager.progressMap[url])
    }
}
