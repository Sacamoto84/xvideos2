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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Менеджер загрузки подробной информации об альбоме (метаданные + список изображений).
 *
 * Координирует:
 * - Загрузку метаданных альбома [AlbumDetails] через GraphQL [getAlbumInfo].
 * - Восстановление и сохранение полного бандла из локального кэша ([restoreBundleIfFresh], [cacheBundleIfComplete]).
 * - Постраничную подгрузку картинок альбома через [AlbumPicsDetails].
 *
 * @property id Уникальный числовой ID альбома.
 * @property repository Репозиторий сетевых запросов и кэша.
 * @property scope CoroutineScope для выполнения сетевых задач.
 */
@Stable
class AlbumInfo(
    val id: Int,
    private val repository: Repository,
    private val scope: CoroutineScope,
) {

    constructor(
        id: Int,
        @Suppress("UNUSED_PARAMETER") download: Boolean,
        repository: Repository,
        scope: CoroutineScope,
    ) : this(id, repository, scope)

    /** Менеджер пагинированной загрузки списка картинок альбома. */
    val albumPicsDetails = AlbumPicsDetails(id, repository)

    private val _albumInfo = MutableStateFlow<AlbumDetails?>(null)
    /** Метаданные альбома (название, автор, описание, теги, счетчик картинок). */
    @Suppress("MemberNameEqualsClassName")
    val albumInfo: StateFlow<AlbumDetails?> = _albumInfo.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    /** Текст последней ошибки загрузки метаданных (если есть). */
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    /** Флаг первичной загрузки информации об альбоме. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    /** Флаг принудительного обновления альбома из сети. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAlbum()
    }

    /** Повторить попытку загрузки альбома при ошибке. */
    fun retry() {
        loadAlbum()
    }

    /** Принудительно обновить данные альбома из сети в обход локального кэша. */
    fun refresh() {
        if (_isRefreshing.value) return
        loadAlbum(forceNetwork = true)
    }

    /**
     * Повторить загрузку страниц картинок, завершившихся сбоем.
     */
    suspend fun retryFailedPages() {
        albumPicsDetails.retryFailedPages()
        val details = _albumInfo.value
        if (details != null && albumPicsDetails.failedPages.isEmpty()) {
            cacheBundleIfComplete(repository, details)
        }
    }

    /**
     * Загружает данные альбома из кэша либо по сети через GraphQL.
     */
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
                    Timber.w("getAlbumInfo $id error: $err")
                    _loadError.value = err
                    _isLoading.value = false
                    return@launch
                }
                val parsed = parseAlbumDetails(result.getOrThrow())

                if (parsed.isFailure) {
                    val err = parsed.exceptionOrNull()?.message ?: "Parse error"
                    Timber.w("getAlbumInfo $id parse error: $err")
                    _loadError.value = err
                    _isLoading.value = false
                    return@launch
                }

                val albumDetails = parsed.getOrThrow()
                _albumInfo.value = albumDetails
                _isLoading.value = false
                Timber.d(
                    "AlbumInfo [$id] Loaded metadata: title='${albumDetails.title}', " +
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

    /**
     * Восстанавливает данные альбома и картинок из кэша бандлов, если кэш актуален.
     */
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
        Timber.d("L album bundle cache hit id:$id items:${bundle.pics.size}")
        return true
    }

    /**
     * Сохраняет снапшот полностью загруженного альбома в файловый бандл-кэш.
     */
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
            Timber.d("L album bundle cache saved id:$id items:${snapshot.pics.size}")
            if (snapshot.pics.size != albumDetails.number_of_pictures) {
                Timber.w(
                    "AlbumInfo [$id] Discrepancy: actual loaded pictures count (${snapshot.pics.size}) " +
                    "!= metadata number_of_pictures (${albumDetails.number_of_pictures})"
                )
            }
        }.onFailure { e ->
            Timber.w(e, "Не удалось сохранить кэш альбома id:$id")
        }
    }

    /**
     * Разбирает JSON-ответ GraphQL-запроса getAlbumInfo.
     */
    private fun parseAlbumDetails(response: String): Result<AlbumDetails> = runCatching {
        val json = LJson.parseToJsonElement(response).jsonObject
        val errors = json["errors"]?.takeIf { it !is JsonNull }?.jsonArray
        if (!errors.isNullOrEmpty()) {
            val errorMsg = errors.firstOrNull()?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                ?: "GraphQL error loading album $id"
            error(errorMsg)
        }
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

    /** Ссылка для скачивания архива всего альбома с сайта (если доступна). */
    val downloadUrl: String
        get() = albumInfo.value?.download_url?.takeIf { it.isNotBlank() }?.let { LusciousEndpoints.HOME + it }.orEmpty()

    val hasError: Boolean get() = loadError.value != null
    val isSuccess: Boolean get() = albumInfo.value != null
}
