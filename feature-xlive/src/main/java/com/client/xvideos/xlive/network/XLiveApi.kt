package com.client.xvideos.xlive.network

import com.client.xvideos.common.net.UserAgentProvider
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.model.XLiveSearchFilters
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.model.XLiveTagSort
import com.client.xvideos.xlive.xLiveUrlStart
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
     * Анкета модели [name]: возраст, откуда, языки, «о себе», подписчики.
     *
     * @throws IOException нет сети, ответ с кодом ошибки или не JSON.
     */
    suspend fun bio(name: String): XLiveBio {
        val response = client.get("$xLiveUrlStart/api/biocontext/${name.encodeURLPathPart()}/")
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLiveBio(response.bodyAsText()) ?: throw IOException("Анкета не JSON")
    }

    /**
     * В эфире ли комната [name]. `true` — модель онлайн, в том числе в
     * привате: открытый ли эфир, скажет только [room]. Комнаты, которой нет,
     * сайт отвечает 404 — это «не в эфире».
     *
     * @throws IOException нет сети, другой код ошибки или ответ без `online`.
     */
    suspend fun isOnline(name: String): Boolean {
        val response = client.get("$xLiveUrlStart/api/online/${name.encodeURLPathPart()}/")
        if (response.status == HttpStatusCode.NotFound) return false
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLiveOnline(response.bodyAsText()) ?: throw IOException("Ответ без online")
    }

    /**
     * Страница списка комнат с [offset]: пол [gender], подборка [collection] и,
     * если заданы, тег [hashtag], слово поиска [keywords] и фильтры [filters].
     *
     * @throws IOException нет сети, ответ с кодом ошибки или без списка комнат.
     */
    suspend fun rooms(
        gender: XLiveGender,
        collection: XLiveCollection,
        offset: Int,
        hashtag: String = "",
        keywords: String = "",
        filters: XLiveSearchFilters = XLiveSearchFilters(),
    ): XLiveRoomPage {
        val query = roomListQuery(gender, collection, offset, hashtag = hashtag, keywords = keywords, filters = filters)
        val response = client.get("$xLiveUrlStart/api/ts/roomlist/room-list/?$query")
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLiveRoomPage(response.bodyAsText()) ?: throw IOException("Ответ без списка комнат")
    }

    /**
     * Страница [page] таблицы тегов: пол [gender], порядок [sort].
     *
     * @throws IOException нет сети, ответ с кодом ошибки или без списка тегов.
     */
    suspend fun tags(gender: XLiveGender, sort: XLiveTagSort, page: Int): XLiveTagPage {
        val response = client.get("$xLiveUrlStart/api/ts/hashtags/tag-table-data/?${tagTableQuery(gender, sort, page)}")
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLiveTagPage(response.bodyAsText()) ?: throw IOException("Ответ без списка тегов")
    }
}
