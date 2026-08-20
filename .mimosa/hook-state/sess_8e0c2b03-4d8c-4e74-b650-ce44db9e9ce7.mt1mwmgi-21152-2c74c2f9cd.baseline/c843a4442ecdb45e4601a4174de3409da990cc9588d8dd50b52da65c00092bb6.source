package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.repository.Repository
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import timber.log.Timber

suspend fun LandingPageAlbumSearch(
    search: String,
    repository: Repository,
    limit: Int = 9,
): Result<Landing_page_albumType> {
    try {
        Timber.i("!!! LandingPageAlbumSearch init search:$search")
        val q = getLandingPageAlbumSearch(search, limit)
        val res = repository.openURI(q)
        val json = JsonParser.parseString(res.getOrThrow()).asJsonObject
        val get = json["data"]?.asJsonObject?.get("landing_page_album")?.asJsonObject?.get("search")?.asJsonObject
        val gson = Gson()
        return Result.success(gson.fromJson(get, Landing_page_albumType::class.java))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.i("!!! eee LandingPageAlbumSearch Exception $e")
        return Result.failure(e)
    }
}
