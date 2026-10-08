package com.client.xvideos.common.webserver

/** Порты ниже 1024 системные: приложению без root их не открыть. */
const val WEB_SERVER_MIN_PORT = 1024
const val WEB_SERVER_MAX_PORT = 65535

/**
 * Разбирает порт веб-сервера из поля ввода.
 *
 * @return Порт в пределах [WEB_SERVER_MIN_PORT]..[WEB_SERVER_MAX_PORT] либо
 * отказ с текстом для пользователя.
 */
fun parseWebServerPort(text: String): Result<Int> {
    val port = text.trim().toIntOrNull()
    return if (port != null && port in WEB_SERVER_MIN_PORT..WEB_SERVER_MAX_PORT) {
        Result.success(port)
    } else {
        Result.failure(IllegalArgumentException("Порт — число от $WEB_SERVER_MIN_PORT до $WEB_SERVER_MAX_PORT"))
    }
}
