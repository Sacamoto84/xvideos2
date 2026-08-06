package com.client.xvideos.l.net

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Content
import com.client.xvideos.l.model.Cover
import com.client.xvideos.l.net.graphQl.getAlbumInfo
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.LusciousEndpoints
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class AlbumInfo(
    val id: Int,
    download: Boolean = false,
    repository: Repository,
    scope: CoroutineScope,
) {

    private companion object {
        val gson = Gson()
    }

    val albumPicsDetails = AlbumPicsDetails(id,  repository)

    val albumInfo = MutableStateFlow(
        AlbumDetails(
            id = "",
            title = "",
            tags = listOf(),
            is_manga = false,
            content = Content("", "", ""),
            genres = listOf(),
            cover = Cover(0, 0, "", ""),
            description = "",
            audiences = listOf(),
            number_of_pictures = 0,
            number_of_animated_pictures = 0,
            url = "",
            download_url = "",
            created = 0.0,
            modified = 0.0
        )
    )

    init {
        scope.launch(Dispatchers.IO) {
            if (restoreBundleIfFresh(repository)) return@launch

            val query = getAlbumInfo(id)
            val result = repository.openURI(query, config = RepositoryUriConfig.DIRECT)
            if (result.isFailure) {
                Timber.w("!!! getAlbumInfo $id error: ${result.exceptionOrNull()?.message}")
                return@launch
            }
            val parsed = parseAlbumDetails(result.getOrThrow())

            if (parsed.isFailure) {
                Timber.w("!!! getAlbumInfo $id parse error: ${parsed.exceptionOrNull()?.message}")
                return@launch
            }

            val albumDetails = parsed.getOrThrow()
            albumInfo.value = albumDetails
            //url = LusciousEndpoints.HOME + albumInfo.value.url
            albumPicsDetails.contentUrls(pageCacheConfig = RepositoryUriConfig.DIRECT)
            cacheBundleIfComplete(repository, albumDetails)
        }
    }

    private suspend fun restoreBundleIfFresh(repository: Repository): Boolean {
        val cachedJson = repository.getAlbumBundleCache(id, L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS) ?: return false
        val bundle = runCatching {
            gson.fromJson(cachedJson, LAlbumBundleCache::class.java)
        }.getOrNull()

        if (
            bundle == null ||
            bundle.schemaVersion != L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION ||
            bundle.pics.isEmpty()
        ) {
            repository.deleteAlbumBundleCache(id)
            return false
        }

        albumInfo.value = bundle.album
        albumPicsDetails.restoreFromBundleCache(
            items = bundle.pics,
            cachedTotalPages = bundle.totalPages
        )
        Timber.i("!!! L album bundle cache hit id:$id items:${bundle.pics.size}")
        return true
    }

    private suspend fun cacheBundleIfComplete(
        repository: Repository,
        albumDetails: AlbumDetails
    ) {
        val snapshot = albumPicsDetails.bundleSnapshotOrNull() ?: return
        val bundle = LAlbumBundleCache(
            schemaVersion = L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION,
            cachedAtMs = System.currentTimeMillis(),
            album = albumDetails,
            totalPages = snapshot.totalPages,
            pics = snapshot.pics
        )
        repository.putAlbumBundleCache(id, gson.toJson(bundle))
        Timber.i("!!! L album bundle cache saved id:$id items:${snapshot.pics.size}")
    }

    private fun parseAlbumDetails(response: String): Result<AlbumDetails> = runCatching {
        val json = JsonParser.parseString(response).asJsonObject
        val get = json["data"]
            ?.asJsonObject
            ?.get("album")
            ?.asJsonObject
            ?.get("get")
            ?.asJsonObject
            ?: error("AlbumInfo response missing data.album.get")
        gson.fromJson(get, AlbumDetails::class.java)
            ?: error("AlbumInfo response data.album.get is empty")
    }

    /**
     * Возвращает url миниатюры альбома.
     * Вычисляется по требованию: `by lazy` зафиксировал бы пустое значение,
     * если бы свойство прочитали до завершения асинхронной загрузки.
     */
    val thumbnail: String get() = albumInfo.value.cover?.url.orEmpty()

    val downloadUrl: String get() = LusciousEndpoints.HOME + albumInfo.value.download_url

//    val artists: List<String> by lazy {
//        tags.filter { it.category == "Artist" }.map { it.name }
//    }
//
//    val characters: List<String> by lazy {
//        tags.filter { it.category == "Character" }.map { it.name }
//    }
//
//    val parodies: List<String> by lazy {
//        tags.filter { it.category == "Parody" }.map { it.name }
//    }

//    val audiences: Map<String, Any> by lazy {
//        json["audiences"] as Map<String, Any>
//    }
////
////    val ongoing: Boolean by lazy {
////        tags.any { it.id == "1895669" && it.text == "ongoing" }
////    }
//
//    val isManga: Boolean by lazy {
//        json["is_manga"] as Boolean
//    }
//
//    val contentType: String by lazy {
//        (json["content"] as Map<*, *>)["title"] as String
//    }

}


