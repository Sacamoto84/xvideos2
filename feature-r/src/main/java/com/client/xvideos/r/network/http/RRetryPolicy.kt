package com.client.xvideos.r.network.http

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpHeaders
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException

/** Сколько раз клиент R повторяет запрос, у которого есть шанс пройти. */
internal const val R_MAX_RETRIES = 2

/**
 * Ответы, после которых повтор имеет смысл: сервер перегружен или не ответил
 * шлюз. Без 500 — это отказ приложения сервера, повторы на нём только
 * оттягивают ошибку на экране.
 */
private val R_RETRY_STATUS_CODES = setOf(429, 502, 503, 504)

/**
 * Повторы запросов клиента R.
 *
 * Раньше повторялось любое исключение, трижды и с растущей паузой. Без сети
 * лента показывала ошибку после четырёх попыток, а при зависшем сервере четыре
 * таймаута по 30 секунд давали около двух минут индикатора. Теперь — как в L:
 * повтор только после обрыва работавшего соединения и отказа шлюза.
 *
 * @param retryDelayMs Пауза между попытками; `null` — растущая, как в приложении.
 */
internal fun HttpClientConfig<*>.installRRetry(retryDelayMs: Long? = null) {
    install(HttpRequestRetry) {
        maxRetries = R_MAX_RETRIES
        retryIf { _, response -> response.status.value in R_RETRY_STATUS_CODES }
        retryOnExceptionIf { _, cause -> cause.isWorthRetrying() }
        if (retryDelayMs == null) exponentialDelay() else constantDelay(millis = retryDelayMs, randomizationMs = 0)
        modifyRequest { if (it.url.pathSegments.contains("auth")) it.headers.remove(HttpHeaders.Authorization) }
    }
}

/**
 * Нет сети, сервер не принимает соединение или не ответил за таймаут —
 * следующая попытка упрётся в то же самое, а пользователь ждёт ошибку.
 */
private fun Throwable.isWorthRetrying(): Boolean = when (this) {
    is UnknownHostException,
    is ConnectException,
    is InterruptedIOException,
    is HttpRequestTimeoutException -> false
    is ResponseException -> response.status.value in R_RETRY_STATUS_CODES
    is IOException -> true
    else -> false
}
