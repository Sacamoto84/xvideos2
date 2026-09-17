package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

suspend fun LandingPageAlbumTag(
    tag: String,
    repository: Repository,
): Result<Landing_page_albumType> {
    try {
        Timber.i("!!! LandingPageAlbumTag init")
        val query = getLandingPageAlbumTag(tag)
        val res = repository.openURI(query)
        val json = LJson.parseToJsonElement(res.getOrThrow()).jsonObject
        val get = json["data"]?.jsonObject?.get("landing_page_album")?.jsonObject?.get("tag")?.jsonObject
            ?: error("LandingPageAlbumTag missing data.landing_page_album.tag")
        return Result.success(LJson.decodeFromJsonElement<Landing_page_albumType>(get))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "LandingPageAlbumTag failed for tag: $tag")
        return Result.failure(e)
    }
}
