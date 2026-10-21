package com.client.xvideos.common.net

import com.client.xvideos.common.eventBus.Event
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Сайт на техобслуживании отвечает 503 и страницей «Maintenance». Раньше любой
 * экран писал только «Не удалось загрузить страницу», и было не понять, что
 * лежит сервер, а не приложение или сеть.
 */
class SiteMaintenanceTest {

    private val request = Request.Builder().url("https://example.com/page").build()

    private fun response(code: Int): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("status")
        .build()

    private fun chain(response: Response): Interceptor.Chain =
        Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "request" -> request
                "proceed" -> response
                else -> null
            }
        } as Interceptor.Chain

    @Test
    fun `ответ 503 сообщает о техобслуживании и уходит дальше без изменений`() {
        var reports = 0
        val original = response(503)

        val result = SiteMaintenanceInterceptor { reports++ }.intercept(chain(original))

        assertEquals(1, reports)
        assertSame(original, result)
    }

    @Test
    fun `другие ответы о техобслуживании не сообщают`() {
        var reports = 0
        val interceptor = SiteMaintenanceInterceptor { reports++ }

        listOf(200, 404, 500, 502).forEach { interceptor.intercept(chain(response(it))) }

        assertEquals(0, reports)
    }

    @Test
    fun `о техобслуживании сайта сообщаем не чаще раза в интервал`() {
        val throttle = SiteMaintenanceThrottle(intervalMs = 120_000L)

        assertTrue(throttle.shouldReport("X", nowMs = 1_000L))
        assertFalse(throttle.shouldReport("X", nowMs = 60_000L))
        assertTrue(throttle.shouldReport("R", nowMs = 60_000L))
        assertTrue(throttle.shouldReport("X", nowMs = 121_000L))
    }

    @Test
    fun `текст сообщения о техобслуживании`() {
        assertEquals(
            "Сайт X на техобслуживании, обещают вернуться через пару минут",
            Event.SiteMaintenance("X").text
        )
    }
}
