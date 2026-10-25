package com.client.xvideos.xlive.network

import com.client.xvideos.common.net.UserAgentProvider
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.xlive.auth.CSRF_COOKIE
import com.client.xvideos.xlive.auth.XLiveWebViewCookieJar
import com.client.xvideos.xlive.auth.webViewUserAgent
import com.client.xvideos.xlive.auth.xLiveCookie
import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveFollowed
import com.client.xvideos.xlive.model.XLiveFollowedCount
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.model.XLiveSearchFilters
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.model.XLiveTagSort
import com.client.xvideos.xlive.xLiveUrlStart
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.util.appendIfNameAbsent
import java.io.IOException

/**
 * Сеть раздела XLive.
 *
 * Клиент процессный: пул соединений живёт между заходами на экран. Почти всё
 * сайт отдаёт анониму; cookie — общие с WebView ([XLiveWebViewCookieJar]),
 * после входа в них сессия, и с ней работают подписки.
 */
object XLiveApi {

    /**
     * User-Agent запросов — тот же, что у WebView: запросы с сессией идут от
     * того же браузера, который входил. Без WebView — обычный браузерный.
     */
    private val userAgent: String by lazy { webViewUserAgent() ?: UserAgentProvider.defaultUserAgent }

    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    dns(AppDns)
                    cookieJar(XLiveWebViewCookieJar)
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            defaultRequest {
                headers.appendIfNameAbsent(HttpHeaders.UserAgent, userAgent)
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

    /**
     * Подписки аккаунта: сначала счётчик — он же проверка сессии, — потом
     * списки в эфире и не в эфире. Без сессии сайт отдал бы на списки общий
     * список комнат, поэтому без счётчика они не запрашиваются.
     *
     * @throws XLiveSignInRequiredException сессии нет или сайт её не узнал.
     * @throws IOException нет сети или ответ не тот.
     */
    suspend fun followed(): XLiveFollowed {
        val count = followedCount()
        if (count.total == 0) return XLiveFollowed()
        return XLiveFollowed(
            online = if (count.online > 0) followedRooms(offline = false) else emptyList(),
            offline = if (count.online < count.total) followedRooms(offline = true) else emptyList(),
        )
    }

    /**
     * Счётчик подписок: сколько всего и сколько в эфире. Без сессии сайт уводит
     * на вход, поэтому ответ — ещё и проверка, что сайт узнаёт сессию.
     *
     * @throws XLiveSignInRequiredException сессии нет или сайт её не узнал.
     * @throws IOException нет сети или ответ без `total`.
     */
    suspend fun followedCount(): XLiveFollowedCount {
        val response = client.get("$xLiveUrlStart/follow/api/online_followed_rooms/")
        response.requireSignedIn()
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLiveFollowedCount(response.bodyAsText()) ?: throw IOException("Ответ без total")
    }

    /** Все страницы подписок в эфире или не в эфире, без промо и повторов. */
    private suspend fun followedRooms(offline: Boolean): List<XLiveRoomItem> {
        val rooms = mutableListOf<XLiveRoomItem>()
        var offset = 0
        for (page in 1..FOLLOWED_MAX_PAGES) {
            val response = client.get("$xLiveUrlStart/api/ts/roomlist/room-list/?${followedRoomsQuery(offline, offset)}")
            if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
            val result = parseXLiveRoomPage(response.bodyAsText()) ?: throw IOException("Ответ без списка комнат")
            val regular = result.rooms.filterNot { it.isPromoted }
            rooms += regular
            offset += ROOM_PAGE_SIZE
            if (regular.isEmpty() || offset >= result.total) break
        }
        return rooms.distinctBy { it.username }
    }

    /**
     * Подписаться на комнату [name] или, при [follow] `false`, отписаться.
     *
     * @return подписка после ответа сайта.
     * @throws XLiveSignInRequiredException сессии нет или сайт её не узнал.
     * @throws IOException нет сети или ответ без `following`.
     */
    suspend fun setFollowing(name: String, follow: Boolean): Boolean {
        val action = if (follow) "follow" else "unfollow"
        val response = postForm(
            path = "/follow/$action/${name.encodeURLPathPart()}/?redirect=error",
            referer = "$xLiveUrlStart/${name.encodeURLPathPart()}/",
            form = mapOf("location" to FOLLOW_LOCATION),
        )
        response.requireSignedIn()
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLiveFollowing(response.bodyAsText()) ?: throw IOException("Ответ без following")
    }

    /**
     * Подписан ли аккаунт на комнату [name].
     *
     * @throws XLiveSignInRequiredException сессии нет или сайт её не узнал.
     * @throws IOException нет сети или ответ без `following`.
     */
    suspend fun isFollowing(name: String): Boolean {
        val response = client.get("$xLiveUrlStart/follow/is_following/${name.encodeURLPathPart()}/")
        response.requireSignedIn()
        if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
        return parseXLiveFollowing(response.bodyAsText()) ?: throw IOException("Ответ без following")
    }

    /**
     * Закрывает сессию на сайте — как кнопка «Выйти» сайта, формой с токеном
     * CSRF. Cookie сессии снимает [com.client.xvideos.xlive.auth.XLiveSession].
     */
    suspend fun signOut() {
        val csrf = xLiveCookie(CSRF_COOKIE) ?: return
        val response = postForm(
            path = "/auth/logout/",
            referer = "$xLiveUrlStart/",
            form = mapOf("csrfmiddlewaretoken" to csrf),
        )
        // Выход сайт подтверждает переадресацией на главную, а по ней после POST
        // клиент не идёт: 3xx — тоже успех.
        val redirected = response.status.value / 100 == REDIRECT_CLASS
        if (!response.status.isSuccess() && !redirected) throw IOException("HTTP ${response.status.value}")
    }

    /**
     * POST формой, как его шлёт сайт: токен CSRF из cookie в заголовке, пометка
     * XHR и адрес страницы — без Referer Django отклоняет POST по HTTPS.
     */
    private suspend fun postForm(path: String, referer: String, form: Map<String, String>): HttpResponse {
        val csrf = xLiveCookie(CSRF_COOKIE) ?: throw XLiveSignInRequiredException("нет csrftoken")
        return client.submitForm(
            url = "$xLiveUrlStart$path",
            formParameters = parameters { form.forEach { (key, value) -> append(key, value) } },
        ) {
            header("X-CSRFToken", csrf)
            header("X-Requested-With", "XMLHttpRequest")
            header(HttpHeaders.Referrer, referer)
            header(HttpHeaders.Origin, xLiveUrlStart)
        }
    }

    /**
     * Без сессии сайт отвечает на запросы подписок 401 или уводит на страницу
     * входа. По переадресации GET клиент идёт сам — она видна по итоговому
     * адресу, — а после POST не идёт, и она видна по `Location`.
     *
     * 403 сюда не относится: так отвечают и Cloudflare, и проверка CSRF, и
     * сессия из-за них сниматься не должна.
     */
    private fun HttpResponse.requireSignedIn() {
        val location = headers[HttpHeaders.Location].orEmpty()
        val toSignIn = request.url.encodedPath.startsWith(SIGN_IN_PATH) || location.contains(SIGN_IN_PATH)
        if (status == HttpStatusCode.Unauthorized || toSignIn) {
            throw XLiveSignInRequiredException(if (toSignIn) "переадресация на вход" else "HTTP ${status.value}")
        }
    }

    /** Страница входа: на неё сайт уводит запросы без сессии. */
    private const val SIGN_IN_PATH = "/auth/login/"

    /** Класс кодов переадресации: 3xx. */
    private const val REDIRECT_CLASS = 3

    /** Сколько страниц подписок брать: по 90 комнат, больше у живого аккаунта не бывает. */
    private const val FOLLOWED_MAX_PAGES = 10

    /** Откуда подписка, для статистики сайта: так её помечает звёздочка подписки на странице комнаты. */
    private const val FOLLOW_LOCATION = "FollowStar"
}
