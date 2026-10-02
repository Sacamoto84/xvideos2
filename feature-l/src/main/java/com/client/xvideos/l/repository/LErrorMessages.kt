package com.client.xvideos.l.repository

import com.client.xvideos.l.LServerErrorException
import java.io.IOException

/** Префикс ошибки [Repository], когда вместо JSON пришла HTML-страница (обычно защита сайта). */
internal const val HTML_INSTEAD_OF_JSON_PREFIX = "Server returned HTML instead of JSON"

/**
 * Текст ошибки раздела L для пользователя.
 *
 * Сырые сообщения не годятся для экрана: при странице защиты в них сотни
 * символов HTML, а сетевые сбои приходят голым «Connection reset».
 */
fun Throwable?.toLUserMessage(): String = when {
    this == null -> UNKNOWN_ERROR
    this is LServerErrorException -> message.orEmpty()
    message?.startsWith(HTML_INSTEAD_OF_JSON_PREFIX) == true ->
        "Сайт L ответил страницей защиты вместо данных, повторите позже"
    this is IOException -> "Нет связи с сервером L: ${message ?: javaClass.simpleName}"
    else -> message?.takeIf { it.isNotBlank() } ?: UNKNOWN_ERROR
}

private const val UNKNOWN_ERROR = "Неизвестная ошибка L"
