package com.client.xvideos.common.net.doh

import java.net.URI

/**
 * Проверяет свой адрес DoH и возвращает его очищенным.
 *
 * Принимается только `https://` с именем сервера. Открытый `http://` для DNS
 * «по HTTPS» — это запросы открытым текстом; адрес без схемы резолвер
 * отбрасывал молча, и запросы уходили другому провайдеру, а экран показывал,
 * что выбран свой.
 */
fun parseDohUrl(raw: String): Result<String> {
    val url = raw.trim()
    val problem = when {
        url.isEmpty() -> "Введите адрес DoH"
        !url.startsWith("https://", ignoreCase = true) -> "Адрес DoH должен начинаться с https://"
        runCatching { URI(url).host }.getOrNull().isNullOrBlank() -> "В адресе DoH нет имени сервера"
        else -> null
    }
    return if (problem == null) Result.success(url) else Result.failure(IllegalArgumentException(problem))
}
