package com.client.xvideos.debug

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    val request = parseGraphQlGet(url) ?: return null
    return "op=${request.operation} vars=${request.variables}"
}

/**
 * Строка выгрузки запроса сайта (JSON Lines): операция, текст запроса после
 * обоих раскодирований (сайт кодирует его дважды), переменные и сырая строка
 * параметров — по ней запрос приложения сверяется с сайтовым байт в байт.
 *
 * @return `null`, если URL — не запрос к GraphQL с параметрами.
 */
internal fun siteRequestDumpLine(url: String): String? {
    val request = parseGraphQlGet(url) ?: return null
    return buildJsonObject {
        put("op", request.operation)
        put("query", request.query)
        put("variables", request.variables)
        put("rawQuery", request.rawQuery)
    }.toString()
}

private class GraphQlGet(val operation: String, val query: String, val variables: String, val rawQuery: String)

private fun parseGraphQlGet(url: String): GraphQlGet? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    if (uri.rawPath?.endsWith(GRAPHQL_PATH_SUFFIX) != true) return null
    val rawQuery = uri.rawQuery ?: return null
    val params = rawQuery.split('&').associate { pair ->
        pair.substringBefore('=') to formDecode(pair.substringAfter('=', ""))
    }
    return GraphQlGet(
        operation = params["operationName"] ?: return null,
        query = formDecode(params["query"].orEmpty()),
        variables = params["variables"].orEmpty(),
        rawQuery = rawQuery,
    )
}

private fun formDecode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())

/**
 * Хост сайта или его поддомен, либо проверка Cloudflare. Всё остальное —
 * реклама и счётчики, их WebView не получает.
 */
internal fun isAllowedHost(host: String, siteDomain: String): Boolean =
    host == siteDomain || host.endsWith(".$siteDomain") || host == CLOUDFLARE_CHALLENGE_HOST

/** Сколько объектов-альбомов в JSON-ответе GraphQL. */
internal fun countAlbums(body: String): Int = ALBUM_TYPENAME_REGEX.findAll(body).count()
