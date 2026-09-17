package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
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
        val json = LJson.parseToJsonElement(res.getOrThrow()).jsonObject
        val get = json["data"]?.jsonObject?.get("landing_page_album")?.jsonObject?.get("search")?.jsonObject
            ?: error("LandingPageAlbumSearch missing data.landing_page_album.search")
        return Result.success(LJson.decodeFromJsonElement<Landing_page_albumType>(get))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "LandingPageAlbumSearch failed for search: $cleanQuery")
        return Result.failure(e)
    }
}
