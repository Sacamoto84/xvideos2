package com.client.xvideos.debug

import java.net.URI
import java.net.URLDecoder

/**
 * Чистые помощники эксперимента [LWebProbeActivity]: что писать в лог и
 * какие хосты пускать в WebView. В лог не попадают ни хост, ни текст
 * запроса — только имя операции и переменные.
 */

private const val GRAPHQL_PATH_SUFFIX = "/graphql/nobatch/"
private const val CLOUDFLARE_CHALLENGE_HOST = "challenges.cloudflare.com"
private val ALBUM_TYPENAME_REGEX = Regex("\"__typename\"\\s*:\\s*\"Album\"")

/**
 * Описание анонимного GET-запроса GraphQL для лога: `op=<операция> vars=<переменные>`.
 *
 * @return `null`, если URL — не запрос к GraphQL с параметрами.
 */
internal fun describeGraphQlRequest(url: String): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    if (uri.rawPath?.endsWith(GRAPHQL_PATH_SUFFIX) != true) return null
    val params = uri.rawQuery?.split('&').orEmpty().associate { pair ->
        val name = pair.substringBefore('=')
        name to URLDecoder.decode(pair.substringAfter('=', ""), Charsets.UTF_8.name())
    }
    val operation = params["operationName"] ?: return null
    return "op=$operation vars=${params["variables"].orEmpty()}"
}

/**
 * Хост сайта или его поддомен, либо проверка Cloudflare. Всё остальное —
 * реклама и счётчики, их WebView не получает.
 */
internal fun isAllowedHost(host: String, siteDomain: String): Boolean =
    host == siteDomain || host.endsWith(".$siteDomain") || host == CLOUDFLARE_CHALLENGE_HOST

/** Сколько объектов-альбомов в JSON-ответе GraphQL. */
internal fun countAlbums(body: String): Int = ALBUM_TYPENAME_REGEX.findAll(body).count()
