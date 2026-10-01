package com.client.xvideos.common.webserver

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Токен доступа к локальному веб-серверу.
 *
 * Сервер слушает все интерфейсы, поэтому без токена библиотеку читал бы любой
 * в той же сети — в обход блокировки приложения. Токен генерируется заново при
 * каждом запуске и попадает только в ссылку и QR-код на экране настроек (он сам
 * за блокировкой). В уведомление его не кладём: оно видно на экране блокировки.
 *
 * Браузер приносит токен параметром [QUERY_PARAM] один раз, дальше сервер
 * узнаёт его по cookie [COOKIE_NAME] (HttpOnly, SameSite=Strict) — так
 * работают `<img>`/`<video>` и `fetch` веб-интерфейса без правок в JS.
 */
internal object WebAccessToken {

    const val QUERY_PARAM = "t"
    const val COOKIE_NAME = "xs_access"

    private const val TOKEN_BYTES = 32

    /** Случайный токен в base64url без паддинга: безопасен и в URL, и в cookie. */
    fun generate(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(TOKEN_BYTES).also { random.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** Сравнение за постоянное время; пустой или отсутствующий токен не совпадает ни с чем. */
    fun matches(presented: String?, expected: String?): Boolean {
        if (presented.isNullOrEmpty() || expected.isNullOrEmpty()) return false
        return MessageDigest.isEqual(presented.toByteArray(Charsets.UTF_8), expected.toByteArray(Charsets.UTF_8))
    }

    /** Ссылка для открытия веб-интерфейса; пустой [baseUrl] (нет сети) остаётся пустым. */
    fun accessUrl(baseUrl: String, token: String): String =
        if (baseUrl.isBlank()) baseUrl else "$baseUrl/?$QUERY_PARAM=$token"
}
