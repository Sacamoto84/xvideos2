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

suspend fun LandingPageAlbumTag(
    tag: String,
    repository: Repository,
): Result<Landing_page_albumType> {
    val cleanTag = tag.trim()
    if (cleanTag.isBlank()) {
        return Result.failure(IllegalArgumentException("Tag cannot be blank"))
    }
    try {
        Timber.d("LandingPageAlbumTag init tag:$cleanTag")
        val query = getLandingPageAlbumTag(cleanTag)
        val res = repository.openURI(query)
        if (res.isFailure) {
            val error = res.exceptionOrNull() ?: IllegalStateException("Failed to load tag: $cleanTag")
            Timber.w("LandingPageAlbumTag request failed: ${error.message}")
            return Result.failure(error)
        }
        val rawJson = res.getOrThrow()
        if (rawJson.isBlank()) {
            return Result.failure(IllegalStateException("Empty response loading tag: $cleanTag"))
        }
        val json = LJson.parseToJsonElement(rawJson).jsonObject
        val errors = json["errors"]?.takeIf { it !is JsonNull }?.jsonArray
        if (!errors.isNullOrEmpty()) {
            val errorMsg = errors.firstOrNull()?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                ?: "GraphQL error loading tag: $cleanTag"
            Timber.w("LandingPageAlbumTag GraphQL error: $errorMsg")
            return Result.failure(IllegalStateException(errorMsg))
        }
        val get = json["data"]?.jsonObject?.get("landing_page_album")?.jsonObject?.get("tag")?.jsonObject
            ?: return Result.failure(IllegalStateException("LandingPageAlbumTag missing data.landing_page_album.tag"))
        return Result.success(LJson.decodeFromJsonElement<Landing_page_albumType>(get))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "LandingPageAlbumTag failed for tag: $cleanTag")
        return Result.failure(e)
    }
}
