package com.client.xvideos.common.coil

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody

// Interceptor для отслеживания прогресса
class ProgressInterceptor(
    private val progressListener: (url: String, bytesRead: Long, contentLength: Long, done: Boolean) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        val url = chain.request().url.toString()

        return originalResponse.newBuilder()
            .body(
                ProgressResponseBody(originalResponse.body) { bytesRead, contentLength, done ->
                    progressListener(url, bytesRead, contentLength, done)
                } as ResponseBody
            )
            .build()
    }
}