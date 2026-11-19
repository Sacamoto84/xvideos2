package com.client.xvideos.n.repository

import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.model.NTagPage
import com.client.xvideos.n.model.NTagSort
import com.client.xvideos.n.model.NTagType
import com.client.xvideos.n.net.NAuthState
import com.client.xvideos.n.net.NClient
import com.client.xvideos.n.net.NNetworkResult
import com.client.xvideos.n.net.NParser
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup

/**
 * Репозиторий данных раздела nhentai с кэшированием в оперативной памяти.
 */
@Singleton
class NRepository @Inject constructor() {

    private val galleryCache = NGalleryCache()
    private val favoriteWrites = NServerFavoriteWrites { galleryId, favorite, apiKey ->
        NClient.setFavorite(galleryId, favorite, apiKey)
    }
    val serverFavoriteRevision = favoriteWrites.revision
    internal val serverFavoriteUpdates = favoriteWrites.updates

    /**
     * Загружает страницу галерей с метаданными пагинации с приоритетом REST API v2.
     */
    suspend fun getGalleriesPage(page: Int = 1, sort: NSortType = NSortType.RECENT): Result<NGalleryPageResult> {
        val apiEndpoint = if (sort == NSortType.RECENT) {
            "/api/v2/galleries?page=$page&per_page=25"
        } else {
            "/api/v2/search?query=-nonexisttag&sort=${sort.queryValue}&page=$page"
        }

        when (val apiResult = NClient.get(apiEndpoint)) {
            is NNetworkResult.Success -> {
                val pageResult = NParser.parseGalleryPageJson(apiResult.body, defaultPage = page)
                if (pageResult.items.isNotEmpty()) return Result.success(pageResult)
            }
            is NNetworkResult.CloudflareRequired -> {
                return Result.failure(apiResult.toException())
            }
            else -> Unit
        }

        // Запасной вариант через HTML-парсинг
        val htmlEndpoint = if (sort == NSortType.RECENT) {
            "/?page=$page"
        } else {
            "/search/?q=-nonexisttag&page=$page&sort=${sort.queryValue}"
        }

        return fetchGalleriesPageFromHtml(htmlEndpoint, defaultPage = page)
    }

    /**
     * Загружает список галерей (лента или сортировки) с приоритетом REST API v2.
     */
    suspend fun getGalleries(page: Int = 1, sort: NSortType = NSortType.RECENT): Result<List<NGalleryPreview>> =
        getGalleriesPage(page, sort).map { it.items }

    /**
     * Поиск галерей по текстовому запросу или тегу со страницей пагинации.
     */
    suspend fun searchPage(query: String, page: Int = 1, sort: NSortType = NSortType.RECENT): Result<NGalleryPageResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val sortParam = if (sort.queryValue.isNotEmpty()) "&sort=${sort.queryValue}" else ""
        val apiEndpoint = "/api/v2/search?query=$encodedQuery&page=$page$sortParam"

        when (val apiResult = NClient.get(apiEndpoint)) {
            is NNetworkResult.Success -> {
                val parsed = runCatching { NParser.parseGalleryPageV2(apiResult.body, page) }
                if (parsed.isSuccess) return parsed
            }
            is NNetworkResult.CloudflareRequired -> {
                return Result.failure(apiResult.toException())
            }
            is NNetworkResult.Error -> if (apiResult.code == 429) {
                return Result.failure(IllegalStateException("Слишком много запросов. Повторите поиск позже."))
            }
            else -> Unit
        }

        // Запасной вариант через HTML-парсинг
        val htmlEndpoint = "/search/?q=$encodedQuery&page=$page$sortParam"
        return fetchGalleriesPageFromHtml(htmlEndpoint, defaultPage = page)
    }

    /**
     * Поиск галерей по текстовому запросу или тегу с приоритетом REST API v2.
     */
    suspend fun search(query: String, page: Int = 1, sort: NSortType = NSortType.RECENT): Result<List<NGalleryPreview>> =
        searchPage(query, page, sort).map { it.items }

    /**
     * Загружает избранные галереи пользователя с сервера (/api/v2/favorites).
     */
    suspend fun getFavoritesPage(page: Int = 1): Result<NGalleryPageResult> {
        val authHint = "Для загрузки избранного укажите действующий API-ключ в настройках N."
        return when (val result = NClient.get("/api/v2/favorites?page=$page")) {
            is NNetworkResult.Success -> runCatching { NParser.parseGalleryPageV2(result.body, page) }
            is NNetworkResult.CloudflareRequired -> Result.failure(result.toException())
            is NNetworkResult.Failure -> Result.failure(result.throwable)
            is NNetworkResult.Error -> when {
                result.code != 401 && result.code != 403 -> Result.failure(IllegalStateException(result.message))
                NAuthState.currentApiKey.isBlank() && NAuthState.currentSessionCookie.isNotBlank() ->
                    fetchGalleriesPageFromHtml("/favorites/?page=$page", page, authHint)
                else -> Result.failure(IllegalStateException(authHint))
            }
        }
    }

    /**
     * Загружает галереи по конкретному тегу с приоритетом эндпоинта /api/v2/galleries/tagged.
     */
    suspend fun getGalleriesByTagPage(
        tag: NTag,
        page: Int = 1,
        sort: NSortType = NSortType.RECENT
    ): Result<NGalleryPageResult> {
        if (tag.id > 0L) {
            val sortParam = if (sort.queryValue.isNotEmpty()) "&sort=${sort.queryValue}" else ""
            val apiEndpoint = "/api/v2/galleries/tagged?tag_id=${tag.id}&page=$page$sortParam"
            when (val apiResult = NClient.get(apiEndpoint)) {
                is NNetworkResult.Success -> {
                    val pageResult = NParser.parseGalleryPageJson(apiResult.body, defaultPage = page)
                    if (pageResult.items.isNotEmpty()) return Result.success(pageResult)
                }
                is NNetworkResult.CloudflareRequired -> {
                    return Result.failure(apiResult.toException())
                }
                else -> Unit
            }
        }

        val tagQuery = when {
            tag.type.isNotEmpty() && tag.type != "tag" -> "${tag.type}:\"${tag.name}\""
            else -> "\"${tag.name}\""
        }
        return searchPage(query = tagQuery, page = page, sort = sort)
    }

    private suspend fun fetchGalleriesPageFromHtml(
        endpoint: String,
        defaultPage: Int,
        favoritesAuthHint: String? = null
    ): Result<NGalleryPageResult> {
        return when (val result = NClient.get(endpoint)) {
            is NNetworkResult.Success -> runCatching {
                if (favoritesAuthHint == null) NParser.parseSearchHtmlPage(result.body, defaultPage)
                else checkNotNull(parseNFavoritesPage(result.finalUrl, result.body, defaultPage)) { favoritesAuthHint }
            }
            is NNetworkResult.CloudflareRequired -> {
                Result.failure(result.toException())
            }
            is NNetworkResult.Error -> {
                Result.failure(RuntimeException("Ошибка HTTP: ${result.code}"))
            }
            is NNetworkResult.Failure -> {
                Result.failure(result.throwable)
            }
        }
    }

    /**
     * Загружает подробную информацию о галерее и похожие работы (API v2 ?include=related).
     */
    suspend fun getGalleryDetails(id: Long, forceRefresh: Boolean = false): Result<NGallery> {
        if (!forceRefresh) galleryCache[id]?.let { return Result.success(it) }

        // Сначала пробуем прямой v2 JSON API с рекомендациями похожих работ
        when (val apiResult = NClient.get("/api/v2/galleries/$id?include=related")) {
            is NNetworkResult.Success -> {
                val gallery = NParser.parseGalleryJson(apiResult.body)
                if (gallery != null && gallery.id == id) {
                    galleryCache[id] = gallery
                    return Result.success(gallery)
                }
            }
            is NNetworkResult.CloudflareRequired -> {
                return Result.failure(apiResult.toException())
            }
            else -> Unit
        }

        // Если JSON API недоступен, пробуем распарсить HTML страницы галереи
        return when (val htmlResult = NClient.get("/g/$id/")) {
            is NNetworkResult.Success -> {
                val gallery = NParser.parseGalleryHtml(htmlResult.body, expectedGalleryId = id)
                if (gallery != null) {
                    galleryCache[id] = gallery
                    Result.success(gallery)
                } else {
                    Result.failure(RuntimeException("Не удалось разобрать данные галереи #$id"))
                }
            }
            is NNetworkResult.CloudflareRequired -> {
                Result.failure(htmlResult.toException())
            }
            is NNetworkResult.Error -> {
                Result.failure(RuntimeException("Ошибка HTTP: ${htmlResult.code}"))
            }
            is NNetworkResult.Failure -> {
                Result.failure(htmlResult.throwable)
            }
        }
    }

    /**
     * Поиск тегов и автодополнение через API v2.
     */
    suspend fun searchTags(query: String, limit: Int = 10, type: NTagType? = null): Result<List<NTag>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return Result.success(emptyList())

        val body = buildJsonObject {
            put("query", cleanQuery)
            put("limit", limit)
            type?.let { put("type", it.value) }
        }.toString()
        return NClient.postJson("/api/v2/tags/search", body).parseTagsResponse(NParser::parseTagsJson)
    }

    suspend fun getTagsPage(type: NTagType, sort: NTagSort, page: Int): Result<NTagPage> =
        NClient.get("/api/v2/tags/${type.value}?sort=${sort.value}&page=$page")
            .parseTagsResponse(NParser::parseTagPage)

    private fun <T> NNetworkResult.parseTagsResponse(parse: (String) -> T): Result<T> = when (this) {
        is NNetworkResult.Success -> runCatching { parse(body) }
        is NNetworkResult.CloudflareRequired -> Result.failure(toException())
        is NNetworkResult.Error -> Result.failure(IllegalStateException("Не удалось загрузить теги (HTTP $code)"))
        is NNetworkResult.Failure -> Result.failure(throwable)
    }

    suspend fun getServerFavorite(galleryId: Long): Result<Boolean> =
        parseServerFavoriteResult(NClient.get("/api/v2/galleries/$galleryId/favorite"))

    suspend fun setServerFavorite(
        galleryId: Long,
        favorite: Boolean,
        apiKey: String = NAuthState.currentApiKey
    ): Result<Boolean> = favoriteWrites.set(galleryId, favorite, apiKey)
}

internal class NGalleryCache(
    private val capacity: Int = 32,
    private val ttlMillis: Long = 5 * 60_000L,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private class Entry(val gallery: NGallery, val loadedAt: Long)
    private val entries = LinkedHashMap<Long, Entry>(capacity, 0.75f, true)

    @Synchronized
    operator fun get(id: Long): NGallery? {
        val entry = entries[id] ?: return null
        if (nowMillis() - entry.loadedAt < ttlMillis) return entry.gallery
        entries.remove(id)
        return null
    }

    @Synchronized
    operator fun set(id: Long, gallery: NGallery) {
        entries[id] = Entry(gallery, nowMillis())
        if (entries.size > capacity) entries.remove(entries.keys.first())
    }
}

internal fun parseNFavoritesPage(url: String, html: String, page: Int): NGalleryPageResult? {
    val target = url.toHttpUrlOrNull() ?: return null
    if (!target.isHttps || target.host != "nhentai.net" || target.encodedPath.trimEnd('/') != "/favorites") return null
    val parsed = NParser.parseSearchHtmlPage(html, page)
    if (parsed.items.isNotEmpty()) return parsed
    val loginForm = Jsoup.parse(html, url).select("form").any {
        val action = it.absUrl("action").toHttpUrlOrNull()
        action?.encodedPath?.trimEnd('/') == "/login" && it.selectFirst("input[type=password]") != null
    }
    return parsed.takeUnless { loginForm }
}

/**
 * Исключение при блокировке запроса проверкой Cloudflare.
 */
class CloudflareBlockedException(val url: String, internal val sentClearance: String = "") :
    RuntimeException("Требуется подтверждение Cloudflare")

internal fun NNetworkResult.CloudflareRequired.toException() = CloudflareBlockedException(challengeUrl, sentClearance)
