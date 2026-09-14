package com.client.xvideos.l.net

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.net.graphQl.getAlbumInfo
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.LusciousEndpoints
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.client.xvideos.l.net.json.LJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

class AlbumInfo(
    val id: Int,
    download: Boolean = false,
    private val repository: Repository,
    private val scope: CoroutineScope,
) {

    val albumPicsDetails = AlbumPicsDetails(id, repository)

    @Suppress("MemberNameEqualsClassName")
    val albumInfo = MutableStateFlow<AlbumDetails?>(null)
    val loadError = MutableStateFlow<String?>(null)
    val isLoading = MutableStateFlow(true)

    init {
        loadAlbum()
    }

    fun retry() {
        loadAlbum()
    }

    private fun loadAlbum() {
        scope.launch(Dispatchers.IO) {
            isLoading.value = true
            loadError.value = null

            if (restoreBundleIfFresh(repository)) {
                isLoading.value = false
                return@launch
            }

            val query = getAlbumInfo(id)
            val result = repository.openURI(query, config = RepositoryUriConfig.DIRECT)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.message ?: "Network error"
                Timber.w("!!! getAlbumInfo $id error: $err")
                loadError.value = err
                isLoading.value = false
                return@launch
            }
            val parsed = parseAlbumDetails(result.getOrThrow())

            if (parsed.isFailure) {
                val err = parsed.exceptionOrNull()?.message ?: "Parse error"
                Timber.w("!!! getAlbumInfo $id parse error: $err")
                loadError.value = err
                isLoading.value = false
                return@launch
            }

            val albumDetails = parsed.getOrThrow()
            albumInfo.value = albumDetails
            isLoading.value = false
            albumPicsDetails.contentUrls(pageCacheConfig = RepositoryUriConfig.DIRECT)
            cacheBundleIfComplete(repository, albumDetails)
        }
    }

    private suspend fun restoreBundleIfFresh(repository: Repository): Boolean {
        val cachedJson = repository.getAlbumBundleCache(id, L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS) ?: return false
        val bundle = runCatching {
            LJson.decodeFromString<LAlbumBundleCache>(cachedJson)
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
        repository.putAlbumBundleCache(id, LJson.encodeToString(bundle))
        Timber.i("!!! L album bundle cache saved id:$id items:${snapshot.pics.size}")
    }

    private fun parseAlbumDetails(response: String): Result<AlbumDetails> = runCatching {
        val json = LJson.parseToJsonElement(response).jsonObject
        val get = json["data"]
            ?.jsonObject
            ?.get("album")
            ?.jsonObject
            ?.get("get")
            ?: error("AlbumInfo response missing data.album.get")
        LJson.decodeFromJsonElement<AlbumDetails>(get)
    }

    /**
     * Возвращает url миниатюры альбома.
     * Вычисляется по требованию: `by lazy` зафиксировал бы пустое значение,
     * если бы свойство прочитали до завершения асинхронной загрузки.
     */
    val thumbnail: String get() = albumInfo.value?.cover?.url.orEmpty()

    val downloadUrl: String get() = LusciousEndpoints.HOME + albumInfo.value?.download_url.orEmpty()

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


