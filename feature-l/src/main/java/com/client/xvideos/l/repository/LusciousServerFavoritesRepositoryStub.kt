package com.client.xvideos.l.repository

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.graphQl.FavoriteAdd
import com.client.xvideos.l.net.graphQl.FavoriteRemove
import com.client.xvideos.l.net.graphQl.FavoritesByDatePicture
import com.client.xvideos.l.net.graphQl.FavoritesByDatePictureSet
import com.client.xvideos.l.net.graphQl.GraphQlRequest
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import com.client.xvideos.l.net.graphQl.refreshMediaCategories
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.net.normalizePictureUrls
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория серверных подписок и лайков Luscious.
 */
@Singleton
class LusciousServerFavoritesRepositoryImpl @Inject constructor(
    private val repository: Repository
) : LusciousServerFavoritesRepository {

    override suspend fun getSessionUserId(): Result<String> {
        // 1. Проверяем текущее значение в mediaCategoriesFlow
        val cachedId = mediaCategoriesFlow.value?.filterSettings?.userId?.takeIf { it > 0 }
        if (cachedId != null) {
            Timber.d("getSessionUserId: found in memory cache: $cachedId")
            return Result.success(cachedId.toString())
        }

        // 2. Принудительно запрашиваем MediaCategoriesBootstrap с сервера без ROM-кеша (DIRECT)
        Timber.d("getSessionUserId: fetching fresh MediaCategoriesBootstrap from server...")
        refreshMediaCategories(repository, forceRefresh = true)
        val freshId = mediaCategoriesFlow.value?.filterSettings?.userId?.takeIf { it > 0 }
        if (freshId != null) {
            Timber.i("getSessionUserId: successfully extracted from session: $freshId")
            return Result.success(freshId.toString())
        }

        return Result.failure(
            IllegalStateException("Пользователь не авторизован в Luscious (user_id не найден в сессии)")
        )
    }

    override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> {
        val targetUserId = if (!userId.isNullOrBlank()) {
            userId
        } else {
            val sessionResult = getSessionUserId()
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: IllegalStateException("Missing user_id"))
            }
            sessionResult.getOrThrow()
        }

        return FavoritesByDatePictureSet(
            repository = repository,
            userId = targetUserId,
            page = page
        )
    }

    override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> {
        val rawResult = getSubscribedAlbumsRaw(userId = null, page = page)
        if (rawResult.isFailure) {
            return Result.failure(rawResult.exceptionOrNull() ?: IllegalStateException("Request failed"))
        }

        return runCatching {
            val raw = rawResult.getOrThrow()
            val json = LJson.parseToJsonElement(raw).jsonObject
            val listByDate = json["data"]?.jsonObject
                ?.get("favorite")?.jsonObject
                ?.get("list_by_date")?.jsonObject

            val errors = listByDate?.get("errors")?.jsonArray
            if (!errors.isNullOrEmpty()) {
                val msg = errors.joinToString { it.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown error" }
                throw IllegalStateException("Server error: $msg")
            }

            val items = listByDate
                ?.get("picture_sets")?.jsonObject
                ?.get("items")

            if (items != null) {
                LJson.decodeFromJsonElement<List<AlbumDetails>>(items)
            } else {
                emptyList()
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to parse subscribed albums response")
        }
    }

    override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> {
        val targetUserId = if (!userId.isNullOrBlank()) {
            userId
        } else {
            val sessionResult = getSessionUserId()
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: IllegalStateException("Missing user_id"))
            }
            sessionResult.getOrThrow()
        }

        return FavoritesByDatePicture(
            repository = repository,
            userId = targetUserId,
            page = page
        )
    }

    override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> {
        val rawResult = getServerLikedPicturesRaw(userId = null, page = page)
        if (rawResult.isFailure) {
            return Result.failure(rawResult.exceptionOrNull() ?: IllegalStateException("Request failed"))
        }

        return runCatching {
            val raw = rawResult.getOrThrow()
            val json = LJson.parseToJsonElement(raw).jsonObject
            val listByDate = json["data"]?.jsonObject
                ?.get("favorite")?.jsonObject
                ?.get("list_by_date")?.jsonObject

            val errors = listByDate?.get("errors")?.jsonArray
            if (!errors.isNullOrEmpty()) {
                val msg = errors.joinToString { it.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown error" }
                throw IllegalStateException("Server error: $msg")
            }

            val itemsElement = listByDate
                ?.get("pictures")?.jsonObject
                ?.get("items")

            if (itemsElement is JsonArray) {
                val sanitized = buildJsonArray {
                    itemsElement.forEach { elem ->
                        if (elem is JsonObject) {
                            val albumElem = elem["album"]
                            val albumId = if (albumElem is JsonObject) {
                                albumElem["id"]?.jsonPrimitive?.contentOrNull
                            } else if (albumElem is JsonPrimitive) {
                                albumElem.contentOrNull
                            } else {
                                null
                            }
                            add(buildJsonObject {
                                elem.forEach { (k, v) ->
                                    if (k == "album") {
                                        put("album", JsonPrimitive(albumId ?: "null"))
                                    } else {
                                        put(k, v)
                                    }
                                }
                            })
                        } else {
                            add(elem)
                        }
                    }
                }
                val pics = LJson.decodeFromJsonElement<List<PicsDetails>>(sanitized)
                normalizePictureUrls(pics)
            } else {
                emptyList()
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to parse server liked pictures response")
        }
    }

    override suspend fun addFavorite(
        anchorId: String,
        anchorType: String,
        favoriteType: String
    ): Result<Unit> {
        val cleanAnchorId = anchorId.trim()
        if (cleanAnchorId.isBlank()) {
            return Result.failure(IllegalArgumentException("anchor_id cannot be blank"))
        }

        val rawResult = FavoriteAdd(
            repository = repository,
            anchorId = cleanAnchorId,
            anchorType = anchorType,
            favoriteType = favoriteType
        )
        if (rawResult.isFailure) {
            return Result.failure(rawResult.exceptionOrNull() ?: IllegalStateException("Request failed"))
        }

        return runCatching {
            val raw = rawResult.getOrThrow()
            val json = LJson.parseToJsonElement(raw).jsonObject

            val rootErrors = json["errors"]?.jsonArray
            if (!rootErrors.isNullOrEmpty()) {
                val msg = rootErrors.joinToString {
                    it.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "GraphQL error"
                }
                throw IllegalStateException(msg)
            }

            val addFavoriteObj = json["data"]?.jsonObject
                ?.get("favorite")?.jsonObject
                ?.get("add_favorite")?.jsonObject

            val mutationErrors = addFavoriteObj?.get("errors")?.jsonArray
            if (!mutationErrors.isNullOrEmpty()) {
                val msg = mutationErrors.joinToString {
                    it.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "Mutation error"
                }
                throw IllegalStateException(msg)
            }
            Unit
        }.onFailure { e ->
            Timber.e(e, "Failed to parse FavoriteAdd response")
        }
    }

    override suspend fun resolvePictureId(
        albumId: String,
        mediaUrlOrFileName: String
    ): Result<String> {
        val cleanAlbumId = albumId.trim()
        val albumInt = cleanAlbumId.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid album id: $albumId"))

        val targetSlug = extractSlugCandidate(mediaUrlOrFileName)
        Timber.d("resolvePictureId: albumId=$cleanAlbumId, targetSlug='$targetSlug' from '$mediaUrlOrFileName'")

        var page = 1
        var totalPages = 1
        while (page <= totalPages && page <= 10) {
            val query = GraphQlRequest.pictureListInsideAlbum(albumInt, page)
            val responseResult = repository.openURI(query, config = RepositoryUriConfig.CACHE_RAM)
            if (responseResult.isFailure) {
                return Result.failure(
                    responseResult.exceptionOrNull() ?: IllegalStateException("Failed to load album page $page")
                )
            }

            val raw = responseResult.getOrThrow()
            val json = runCatching { LJson.parseToJsonElement(raw).jsonObject }.getOrNull()
                ?: return Result.failure(IllegalStateException("Malformed album JSON"))

            val pictureList = json["data"]?.jsonObject
                ?.get("picture")?.jsonObject
                ?.get("list")?.jsonObject
                ?: return Result.failure(IllegalStateException("Missing picture list"))

            val info = pictureList["info"]?.jsonObject
            totalPages = info?.get("total_pages")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: totalPages

            val items = pictureList["items"]?.jsonArray.orEmpty()
            for (element in items) {
                val picObj = element.jsonObject
                val picId = picObj["id"]?.jsonPrimitive?.contentOrNull?.trim()
                if (picId.isNullOrBlank()) continue

                val picUrl = picObj["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val picOrig = picObj["url_to_original"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val picVideo = picObj["url_to_video"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val thumbUrls = picObj["thumbnails"]?.jsonArray.orEmpty().mapNotNull {
                    it.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                }

                val allPicUrls = listOf(picUrl, picOrig, picVideo) + thumbUrls
                if (targetSlug.isNotBlank() && allPicUrls.any { it.contains(targetSlug, ignoreCase = true) }) {
                    Timber.d("resolvePictureId: matched slug '$targetSlug' with picture id: $picId")
                    return Result.success(picId)
                }
            }
            page++
        }
        return Result.failure(IllegalStateException("Picture ID not found in album $albumId"))
    }

    override suspend fun removeFavorite(
        anchorId: String,
        anchorType: String,
        favoriteType: String
    ): Result<Unit> {
        val cleanAnchorId = anchorId.trim()
        if (cleanAnchorId.isBlank()) {
            return Result.failure(IllegalArgumentException("anchor_id cannot be blank"))
        }

        val rawResult = FavoriteRemove(repository).removeFavorite(
            anchorId = cleanAnchorId,
            anchorType = anchorType,
            favoriteType = favoriteType
        )
        if (rawResult.isFailure) {
            return Result.failure(rawResult.exceptionOrNull() ?: IllegalStateException("Request failed"))
        }

        return runCatching {
            val raw = rawResult.getOrThrow()
            val json = LJson.parseToJsonElement(raw).jsonObject

            val rootErrors = json["errors"]?.jsonArray
            if (!rootErrors.isNullOrEmpty()) {
                val msg = rootErrors.joinToString {
                    it.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "GraphQL error"
                }
                throw IllegalStateException(msg)
            }

            val removeFavoriteObj = json["data"]?.jsonObject
                ?.get("favorite")?.jsonObject
                ?.get("remove_favorite")?.jsonObject

            val mutationErrors = removeFavoriteObj?.get("errors")?.jsonArray
            if (!mutationErrors.isNullOrEmpty()) {
                val msg = mutationErrors.joinToString {
                    it.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "Mutation error"
                }
                throw IllegalStateException(msg)
            }
            Unit
        }.onFailure { e ->
            Timber.e(e, "Failed to parse FavoriteRemove response")
        }
    }
}

internal fun extractSlugCandidate(input: String): String {
    val clean = input.substringBefore('?').substringBefore('#').trim()
    val ulidMatch = Regex("""[0-9A-HJKMNP-TV-Z]{26}""").find(clean)
    if (ulidMatch != null) return ulidMatch.value

    val fileName = clean.substringAfterLast('/').substringAfterLast('\\')
    val withoutExt = fileName
        .replace(Regex("""\.\d+x\d+\.[a-zA-Z0-9]+$"""), "")
        .replace(Regex("""\.[a-zA-Z0-9]+$"""), "")

    val slug = withoutExt.substringAfterLast('_')
    return if (slug.length >= 4) slug else withoutExt
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LusciousServerFavoritesModule {

    @Binds
    @Singleton
    abstract fun bindLusciousServerFavoritesRepository(
        impl: LusciousServerFavoritesRepositoryImpl
    ): LusciousServerFavoritesRepository
}
