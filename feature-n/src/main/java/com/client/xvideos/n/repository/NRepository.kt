package com.client.xvideos.n.repository

import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NSortType
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
     * Загружает список галерей (лента или сортировки).
     */
    suspend fun getGalleries(page: Int = 1, sort: NSortType = NSortType.RECENT): Result<List<NGalleryPreview>> {
        val endpoint = if (sort.queryValue.isEmpty()) {
            "/?page=$page"
        } else {
            "/search/?q=-nonexisttag&page=$page&sort=${sort.queryValue}"
        }

        return when (val result = NClient.get(endpoint)) {
            is NNetworkResult.Success -> {
                val galleries = NParser.parseSearchHtml(result.body)
                Result.success(galleries)
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
     * Поиск галерей по текстовому запросу или тегу.
     */
    suspend fun search(query: String, page: Int = 1, sort: NSortType = NSortType.RECENT): Result<List<NGalleryPreview>> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val sortParam = if (sort.queryValue.isNotEmpty()) "&sort=${sort.queryValue}" else ""
        val endpoint = "/search/?q=$encodedQuery&page=$page$sortParam"

        return when (val result = NClient.get(endpoint)) {
            is NNetworkResult.Success -> {
                val galleries = NParser.parseSearchHtml(result.body)
                Result.success(galleries)
            }
            is NNetworkResult.CloudflareRequired -> {
                Result.failure(CloudflareBlockedException(result.challengeUrl))
            }
            is NNetworkResult.Error -> {
                Result.failure(RuntimeException("Ошибка поиска HTTP: ${result.code}"))
            }
            is NNetworkResult.Failure -> {
                Result.failure(result.throwable)
            }
        }
    }

    /**
     * Загружает подробную информацию о галерее (сначала из кэша, затем из сети).
     */
    suspend fun getGalleryDetails(id: Long): Result<NGallery> {
        galleryCache[id]?.let { return Result.success(it) }

        // Сначала пробуем прямой v2 JSON API
        when (val apiResult = NClient.get("/api/v2/galleries/$id")) {
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
}

/**
 * Исключение при блокировке запроса проверкой Cloudflare.
 */
class CloudflareBlockedException(val url: String) : RuntimeException("Требуется подтверждение Cloudflare: $url")
