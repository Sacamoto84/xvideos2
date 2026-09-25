package com.client.xvideos.common.coil

import okhttp3.Interceptor
import okhttp3.Response

// Interceptor для отслеживания прогресса
class ProgressInterceptor(
    private val progressListener: (url: String, bytesRead: Long, contentLength: Long, done: Boolean) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        if (!originalResponse.isSuccessful || originalResponse.code == 204 || originalResponse.code == 304) {
            return originalResponse
        }
        @Suppress("USELESS_ELVIS")
        val body = originalResponse.body ?: return originalResponse
        val url = chain.request().url.toString()

        return originalResponse.newBuilder()
            .body(
                ProgressResponseBody(body) { bytesRead, contentLength, done ->
                    progressListener(url, bytesRead, contentLength, done)
                }
            )
            .build()
    }
}
