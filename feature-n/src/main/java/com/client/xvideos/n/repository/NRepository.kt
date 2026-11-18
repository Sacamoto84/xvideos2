package com.client.xvideos.n.repository

import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.net.NClient
import com.client.xvideos.n.net.NNetworkResult
import com.client.xvideos.n.net.NParser
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий данных раздела nhentai с кэшированием в оперативной памяти.
 */
@Singleton
class NRepository @Inject constructor() {

    private val galleryCache = ConcurrentHashMap<Long, NGallery>()

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
                return Result.failure(CloudflareBlockedException(apiResult.challengeUrl))
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
                val pageResult = NParser.parseGalleryPageJson(apiResult.body, defaultPage = page)
                if (pageResult.items.isNotEmpty()) return Result.success(pageResult)
            }
            is NNetworkResult.CloudflareRequired -> {
                return Result.failure(CloudflareBlockedException(apiResult.challengeUrl))
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
        val apiEndpoint = "/api/v2/favorites?page=$page"
        when (val apiResult = NClient.get(apiEndpoint)) {
            is NNetworkResult.Success -> {
                val pageResult = NParser.parseGalleryPageJson(apiResult.body, defaultPage = page)
                return Result.success(pageResult)
            }
            is NNetworkResult.CloudflareRequired -> {
                return Result.failure(CloudflareBlockedException(apiResult.challengeUrl))
            }
            else -> Unit
        }

        // Запасной вариант через HTML-парсинг
        val htmlEndpoint = "/favorites/?page=$page"
        return fetchGalleriesPageFromHtml(htmlEndpoint, defaultPage = page)
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
                    return Result.failure(CloudflareBlockedException(apiResult.challengeUrl))
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

    private suspend fun fetchGalleriesPageFromHtml(endpoint: String, defaultPage: Int): Result<NGalleryPageResult> {
        return when (val result = NClient.get(endpoint)) {
            is NNetworkResult.Success -> {
                val pageResult = NParser.parseSearchHtmlPage(result.body, defaultPage = defaultPage)
                Result.success(pageResult)
            }
            is NNetworkResult.CloudflareRequired -> {
                Result.failure(CloudflareBlockedException(result.challengeUrl))
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
    suspend fun getGalleryDetails(id: Long): Result<NGallery> {
        galleryCache[id]?.let { return Result.success(it) }

        // Сначала пробуем прямой v2 JSON API с рекомендациями похожих работ
        when (val apiResult = NClient.get("/api/v2/galleries/$id?include=related")) {
            is NNetworkResult.Success -> {
                val gallery = NParser.parseGalleryJson(apiResult.body)
                if (gallery != null) {
                    galleryCache[id] = gallery
                    return Result.success(gallery)
                }
            }
            is NNetworkResult.CloudflareRequired -> {
                return Result.failure(CloudflareBlockedException(apiResult.challengeUrl))
            }
            else -> Unit
        }

        // Если JSON API недоступен, пробуем распарсить HTML страницы галереи
        return when (val htmlResult = NClient.get("/g/$id/")) {
            is NNetworkResult.Success -> {
                val gallery = NParser.parseGalleryHtml(htmlResult.body)
                if (gallery != null) {
                    galleryCache[id] = gallery
                    Result.success(gallery)
                } else {
                    Result.failure(RuntimeException("Не удалось разобрать данные галереи #$id"))
                }
            }
            is NNetworkResult.CloudflareRequired -> {
                Result.failure(CloudflareBlockedException(htmlResult.challengeUrl))
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
    suspend fun searchTags(query: String, limit: Int = 10): Result<List<NTag>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return Result.success(emptyList())

        val escapedQuery = cleanQuery.replace("\"", "\\\"")
        val jsonBody = """{"query":"$escapedQuery","limit":$limit}"""

        return when (val result = NClient.postJson("/api/v2/tags/search", jsonBody)) {
            is NNetworkResult.Success -> {
                val tags = NParser.parseTagsJson(result.body)
                Result.success(tags)
            }
            is NNetworkResult.CloudflareRequired -> {
                Result.failure(CloudflareBlockedException(result.challengeUrl))
            }
            is NNetworkResult.Error -> {
                Result.failure(RuntimeException("Ошибка поиска тегов: ${result.code}"))
            }
            is NNetworkResult.Failure -> {
                Result.failure(result.throwable)
            }
        }
    }

    /**
     * Переключает состояние избранного для галереи на сервере (/g/{id}/favorite).
     */
    suspend fun toggleServerFavorite(galleryId: Long): Result<Boolean> {
        val endpoint = "/g/$galleryId/favorite"
        return when (val result = NClient.postForm(endpoint)) {
            is NNetworkResult.Success -> {
                val isFav = result.body.contains("\"favorited\": true") ||
                    result.body.contains("\"favorited\":true")
                Result.success(isFav)
            }
            is NNetworkResult.CloudflareRequired -> {
                Result.failure(CloudflareBlockedException(result.challengeUrl))
            }
            is NNetworkResult.Error -> {
                Result.failure(RuntimeException("Ошибка обновления избранного на сервере: ${result.code}"))
            }
            is NNetworkResult.Failure -> {
                Result.failure(result.throwable)
            }
        }
    }

    /**
     * Получает ID случайной манги через API v2.
     */
    suspend fun getRandomGalleryId(): Result<Long> {
        return when (val result = NClient.get("/api/v2/galleries/random")) {
            is NNetworkResult.Success -> {
                val id = NParser.parseRandomGalleryId(result.body)
                if (id != null && id > 0L) {
                    Result.success(id)
                } else {
                    Result.failure(RuntimeException("Не удалось получить ID случайной манги"))
                }
            }
            is NNetworkResult.CloudflareRequired -> {
                Result.failure(CloudflareBlockedException(result.challengeUrl))
            }
            is NNetworkResult.Error -> {
                Result.failure(RuntimeException("Ошибка получения случайной манги: ${result.code}"))
            }
            is NNetworkResult.Failure -> {
                Result.failure(result.throwable)
            }
        }
    }
}

/**
 * Исключение при блокировке запроса проверкой Cloudflare.
 */
class CloudflareBlockedException(val url: String) : RuntimeException("Требуется подтверждение Cloudflare: $url")
