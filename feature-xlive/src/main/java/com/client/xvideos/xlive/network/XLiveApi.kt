package com.client.xvideos.xlive.network

import com.client.xvideos.common.net.UserAgentProvider
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.xLiveUrlStart
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.util.appendIfNameAbsent
import java.io.IOException

/**
 * Сеть раздела XLive.
 *
 * Клиент процессный: пул соединений живёт между заходами на экран. Cookie и
 * вход не нужны — контекст открытой комнаты сайт отдаёт анониму.
 */
object XLiveApi {

    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    dns(AppDns)
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            defaultRequest {
                headers.appendIfNameAbsent(HttpHeaders.UserAgent, UserAgentProvider.defaultUserAgent)
            }
        }
    }

    /**
     * Комната [name]: статус и адрес потока.
     *
     * Токен в адресе живёт минуты, а сессия потока гаснет без запросов, поэтому
     * адрес берётся перед каждым запуском плеера, а не хранится.
     *
     * @throws IOException нет сети, ответ с кодом ошибки или без `room_status`.
     */
    suspend fun room(name: String): XLiveRoom {
        val response = client.get("$xLiveUrlStart/api/chatvideocontext/${name.encodeURLPathPart()}/")
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLiveRoom(response.bodyAsText()) ?: throw IOException("Ответ без room_status")
    }

    /**
     * Ники открытых комнат с первой страницы списка, самые популярные первыми.
     *
     * @throws IOException нет сети или ответ с кодом ошибки.
     */
    suspend fun publicRooms(limit: Int = ROOM_LIST_LIMIT): List<String> {
        val response = client.get("$xLiveUrlStart/api/ts/roomlist/room-list/?limit=$limit&offset=0")
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLivePublicRooms(response.bodyAsText())
    }

    private const val ROOM_LIST_LIMIT = 10
}
