package com.client.xvideos.l.net

import androidx.compose.runtime.Stable
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.net.graphQl.getAlbumInfo
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.LusciousEndpoints
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.client.xvideos.l.net.json.LJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import timber.log.Timber

@Stable
class AlbumInfo(
    val id: Int,
    download: Boolean = false,
    private val repository: Repository,
    private val scope: CoroutineScope,
) {

    val albumPicsDetails = AlbumPicsDetails(id, repository)

    private val _albumInfo = MutableStateFlow<AlbumDetails?>(null)
    @Suppress("MemberNameEqualsClassName")
    val albumInfo: StateFlow<AlbumDetails?> = _albumInfo.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAlbum()
    }

    fun retry() {
        loadAlbum()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        loadAlbum(forceNetwork = true)
    }

    suspend fun retryFailedPages() {
        albumPicsDetails.retryFailedPages()
        val details = _albumInfo.value
        if (details != null && albumPicsDetails.failedPages.isEmpty()) {
            cacheBundleIfComplete(repository, details)
        }
    }

    private fun loadAlbum(forceNetwork: Boolean = false) {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.IO) {
            if (forceNetwork) {
                _isRefreshing.value = true
                repository.deleteAlbumBundleCache(id)
            } else {
                _isLoading.value = true
            }
            _loadError.value = null

            try {
                if (!forceNetwork && restoreBundleIfFresh(repository)) {
                    _isLoading.value = false
                    return@launch
                }

                val query = getAlbumInfo(id)
                val result = repository.openURI(query, config = RepositoryUriConfig.DIRECT)
                if (result.isFailure) {
                    val err = result.exceptionOrNull()?.message ?: "Network error"
                    Timber.w("!!! getAlbumInfo $id error: $err")
                    _loadError.value = err
                    _isLoading.value = false
                    return@launch
                }
                val parsed = parseAlbumDetails(result.getOrThrow())

                if (parsed.isFailure) {
                    val err = parsed.exceptionOrNull()?.message ?: "Parse error"
                    Timber.w("!!! getAlbumInfo $id parse error: $err")
                    _loadError.value = err
                    _isLoading.value = false
                    return@launch
                }

                val albumDetails = parsed.getOrThrow()
                _albumInfo.value = albumDetails
                _isLoading.value = false
                Timber.i(
                    "!!! AlbumInfo [$id] Loaded metadata: title='${albumDetails.title}', " +
                    "pictures=${albumDetails.number_of_pictures}, " +
                    "animated=${albumDetails.number_of_animated_pictures}, " +
                    "description='${albumDetails.description}'"
                )
                albumPicsDetails.contentUrls(pageCacheConfig = RepositoryUriConfig.DIRECT)
                cacheBundleIfComplete(repository, albumDetails)
            } finally {
                if (forceNetwork) {
                    _isRefreshing.value = false
                }
            }
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

        albumPicsDetails.restoreFromBundleCache(
            items = bundle.pics,
            cachedTotalPages = bundle.totalPages
        )
        _albumInfo.value = bundle.album
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
        runCatching {
            repository.putAlbumBundleCache(id, LJson.encodeToString(bundle))
            Timber.i("!!! L album bundle cache saved id:$id items:${snapshot.pics.size}")
            if (snapshot.pics.size != albumDetails.number_of_pictures) {
                Timber.w(
                    "!!! AlbumInfo [$id] Discrepancy: actual loaded pictures count (${snapshot.pics.size}) " +
                    "!= metadata number_of_pictures (${albumDetails.number_of_pictures})"
                )
            }
        }.onFailure { e ->
            Timber.w(e, "Не удалось сохранить кэш альбома id:$id")
        }
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


