package com.client.xvideos.common.coil

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class ProgressInterceptorTest {

    private fun mockChain(request: Request, response: Response): Interceptor.Chain {
        return Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "request" -> request
                "proceed" -> response
                else -> null
            }
        } as Interceptor.Chain
    }

    @Test
    fun `ответ с кодом 304 Not Modified не оборачивается в ProgressResponseBody`() {
        var progressCalled = false
        val interceptor = ProgressInterceptor { _, _, _, _ ->
            progressCalled = true
        }

        val request = Request.Builder()
            .url("https://example.com/image.jpg")
            .build()

        val responseWithoutBody = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(304)
            .message("Not Modified")
            .build()

        val chain = mockChain(request, responseWithoutBody)

        val result = interceptor.intercept(chain)
        assertEquals(304, result.code)
        assertTrue(result.body !is ProgressResponseBody)
        assertEquals(false, progressCalled)
    }

    @Test
    fun `ответ с телом отслеживает прогресс`() {
        var recordedBytes = 0L
        val interceptor = ProgressInterceptor { _, bytes, _, _ ->
            recordedBytes = bytes
        }

        val request = Request.Builder()
            .url("https://example.com/image.jpg")
            .build()

        val responseWithBody = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("hello world".toResponseBody("text/plain".toMediaType()))
            .build()

        val chain = mockChain(request, responseWithBody)

        val result = interceptor.intercept(chain)
        assertEquals(200, result.code)
        val bodyText = result.body.string()
        assertEquals("hello world", bodyText)
        assertEquals(11L, recordedBytes)
    }
}
