package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.Repository
import com.google.gson.Gson
import com.google.gson.JsonParser
import timber.log.Timber

suspend fun LandingPageAlbumTag(
    tag: String,
    repository: Repository,
): Result<Landing_page_albumType> {
    try {
        Timber.i("!!! LandingPageAlbumTag init")
        val q = getLandingPageAlbumTag(tag)
        val res = repository.openURI(q)
        val json = JsonParser.parseString(res.getOrThrow()).asJsonObject
        val get =  json["data"]?.asJsonObject?.get("landing_page_album")?.asJsonObject?.get("tag")?.asJsonObject
        val gson = Gson()
        return Result.success(gson.fromJson(get, Landing_page_albumType::class.java))
    } catch (e: Exception) {
        Timber.i("!!! eee LandingPageAlbumTag Exception $e")
        return Result.failure(e)
    }
}