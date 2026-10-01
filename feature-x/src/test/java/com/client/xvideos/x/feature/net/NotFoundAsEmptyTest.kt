package com.client.xvideos.x.feature.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class NotFoundAsEmptyTest {

    @Test
    fun `404 даёт пустую строку`() {
        assertEquals("", notFoundAsEmpty { throw HttpStatusException(404, "https://x.test/a") })
    }

    @Test
    fun `прочие коды ответа и сбой сети пробрасываются`() {
        assertThrows(HttpStatusException::class.java) {
            notFoundAsEmpty { throw HttpStatusException(503, "https://x.test/a") }
        }
        assertThrows(IOException::class.java) {
            notFoundAsEmpty { throw IOException("нет сети") }
        }
    }

    @Test
    fun `успешный ответ возвращается как есть`() {
        assertEquals("<html/>", notFoundAsEmpty { "<html/>" })
    }
}
