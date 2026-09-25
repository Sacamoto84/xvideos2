package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

suspend fun LandingPageAlbumSearch(
    search: String,
    repository: Repository,
    limit: Int = 9,
): Result<Landing_page_albumType> {
    val cleanQuery = search.trim()
    if (cleanQuery.isBlank()) {
        return Result.failure(IllegalArgumentException("Search query cannot be blank"))
    }
    val safeLimit = limit.coerceAtLeast(1)
    try {
        Timber.d("LandingPageAlbumSearch init search:$cleanQuery limit:$safeLimit")
        val query = getLandingPageAlbumSearch(cleanQuery, safeLimit)
        val res = repository.openURI(query)
        if (res.isFailure) {
            val error = res.exceptionOrNull() ?: IllegalStateException("Failed to search albums: $cleanQuery")
            Timber.w("LandingPageAlbumSearch request failed: ${error.message}")
            return Result.failure(error)
        }
        val rawJson = res.getOrThrow()
        if (rawJson.isBlank()) {
            return Result.failure(IllegalStateException("Empty response searching albums: $cleanQuery"))
        }
        val json = LJson.parseToJsonElement(rawJson).jsonObject
        val errors = json["errors"]?.takeIf { it !is JsonNull }?.jsonArray
        if (!errors.isNullOrEmpty()) {
            val errorMsg = errors.firstOrNull()?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                ?: "GraphQL error searching albums: $cleanQuery"
            Timber.w("LandingPageAlbumSearch GraphQL error: $errorMsg")
            return Result.failure(IllegalStateException(errorMsg))
        }
        val get = json["data"]?.jsonObject?.get("landing_page_album")?.jsonObject?.get("search")?.jsonObject
            ?: return Result.failure(IllegalStateException("LandingPageAlbumSearch missing data.landing_page_album.search"))
        return Result.success(LJson.decodeFromJsonElement<Landing_page_albumType>(get))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "LandingPageAlbumSearch failed for search: $cleanQuery")
        return Result.failure(e)
    }
}
