package com.client.xvideos.r.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Текст ошибки раздела R для пользователя.
 *
 * Сырые сообщения не годятся для экрана: у сбоев сети в них адрес сервера, у
 * отказов сервера — адрес запроса целиком, а `toString()` исключения начинается
 * с имени его класса.
 */
fun Throwable?.toRUserMessage(): String = when {
    this == null -> UNKNOWN_ERROR
    this is ResponseException -> "Сервер R ответил ошибкой (HTTP ${response.status.value})"
    this is HttpRequestTimeoutException || this is InterruptedIOException -> "Сервер R не ответил вовремя"
    this is IOException -> "Нет связи с сервером R"
    else -> message?.takeIf { it.isNotBlank() } ?: UNKNOWN_ERROR
}

private const val UNKNOWN_ERROR = "Неизвестная ошибка R"
