package com.client.xvideos.l.repository

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.graphQl.FavoritesByDatePicture
import com.client.xvideos.l.net.graphQl.FavoritesByDatePictureSet
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
