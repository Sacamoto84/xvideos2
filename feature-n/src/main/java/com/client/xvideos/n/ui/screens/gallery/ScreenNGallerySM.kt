package com.client.xvideos.n.ui.screens.gallery

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.net.NAuthState
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.saved.NFavoritesRepository
import com.client.xvideos.n.saved.NSaved
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ScreenModel экрана деталей манги.
 */
@Stable
class ScreenNGallerySM @AssistedInject constructor(
    @Assisted val galleryId: Long,
    private val repository: NRepository,
    private val favoritesRepository: NFavoritesRepository,
    val saved: NSaved
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(galleryId: Long): ScreenNGallerySM
    }

    private val _uiState = MutableStateFlow<NGalleryUiState>(NGalleryUiState.Loading)
    val uiState: StateFlow<NGalleryUiState> = _uiState.asStateFlow()

    internal val localFavorites = favoritesRepository.favorites

    private val _serverFavorite = MutableStateFlow(NServerFavoriteState(loading = NAuthState.currentApiKey.isNotBlank()))
    val serverFavorite: StateFlow<NServerFavoriteState> = _serverFavorite.asStateFlow()
    private val _favoriteChallenge = MutableStateFlow<CloudflareBlockedException?>(null)
    internal val favoriteChallenge = _favoriteChallenge.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field
    val pagesPosition = LazyGridPosition()

    var isSelectCollectionOpen by mutableStateOf(false)
    var isCreateCollectionOpen by mutableStateOf(false)
    var targetPictureForCollection by mutableStateOf<NPicture?>(null)

    private val favoriteStatusRefresh = MutableStateFlow(0L)
    private var loadJob: Job? = null
    private var favoriteJob: Job? = null
    private var serverFavoriteWrite: Job? = null
    private var serverFavoriteApiKey = NAuthState.currentApiKey
    private var serverFavoriteVersion = 0L
    private var retryFavorite: (() -> Unit)? = null

    internal fun scrollToReaderPage(pageNumber: Int) {
        screenModelScope.launch {
            val state = uiState.first { it !is NGalleryUiState.Loading } as? NGalleryUiState.Success ?: return@launch
            val pageIndex = state.gallery.pages.indexOfFirst { it.pageNumber == pageNumber }
            if (pageIndex < 0) return@launch
            // Первый элемент сетки занимает заголовок альбома.
            pagesPosition.scrollToItem(pageIndex + 1)
        }
    }

    fun openAddToCollection(picture: NPicture) {
        targetPictureForCollection = picture
        isSelectCollectionOpen = true
    }

    fun addToCollection(collectionName: String, create: Boolean = false) {
        val picture = targetPictureForCollection ?: return
        screenModelScope.launch {
            if (create) saved.collection.createCollectionAndAdd(collectionName, picture)
            else saved.collection.addToCollection(collectionName, picture)
        }
        isSelectCollectionOpen = false
        isCreateCollectionOpen = false
        targetPictureForCollection = null
    }

    fun getCollectionCoverUrl(collectionName: String, items: List<NPicture>): String? =
        saved.collection.getCollectionCoverUrl(collectionName, items)

    fun isPageLiked(pageNumber: Int): Boolean? =
        saved.likes.isLiked(galleryId, pageNumber)

    fun setLiked(picture: NPicture, liked: Boolean) {
        screenModelScope.launch { saved.likes.setLiked(picture, liked) }
    }

    init {
        loadGallery(forceRefresh = false)
        screenModelScope.launch {
            repository.serverFavoriteUpdates.collect { update ->
                if (update.galleryId == galleryId && update.apiKey == NAuthState.currentApiKey) {
                    selectServerFavoriteAccount(update.apiKey)
                    serverFavoriteVersion++
                    if (update.saved == null) refreshServerFavorite()
                    else _serverFavorite.value = NServerFavoriteState(saved = update.saved)
                }
            }
        }
        screenModelScope.launch {
            combine(Settings.n_api_key.field, favoriteStatusRefresh) { key, _ -> key }.collectLatest { key ->
                selectServerFavoriteAccount(key)
                if (key.isNotBlank()) {
                    serverFavoriteWrite?.join()
                    readServerFavorite(key)
                }
            }
        }
    }

    fun toggleFavorite() {
        val gallery = (_uiState.value as? NGalleryUiState.Success)?.gallery ?: return
        if (localFavorites.value == null || favoriteJob?.isActive == true) return
        favoriteJob = screenModelScope.launch {
            withContext(Dispatchers.IO) { favoritesRepository.toggleFavorite(gallery) }
                .onFailure { SnackBar.error("Не удалось изменить локальное избранное") }
        }
    }

    private fun selectServerFavoriteAccount(apiKey: String) {
        if (serverFavoriteApiKey == apiKey) return
        serverFavoriteApiKey = apiKey
        serverFavoriteVersion++
        _serverFavorite.value = NServerFavoriteState(loading = apiKey.isNotBlank())
        finishFavoriteChallenge(verified = false)
    }

    private fun refreshServerFavorite() {
        _serverFavorite.value = _serverFavorite.value.copy(loading = true)
        favoriteStatusRefresh.value++
    }

    private suspend fun readServerFavorite(apiKey: String) {
        if (NAuthState.currentApiKey != apiKey) return
        val version = serverFavoriteVersion
        val previous = _serverFavorite.value
        _serverFavorite.value = previous.copy(loading = true)
        val result = repository.getServerFavorite(galleryId)
        if (NAuthState.currentApiKey == apiKey && version == serverFavoriteVersion) {
            _serverFavorite.value = previous.afterRead(result)
        }
    }

    fun toggleServerFavorite() {
        val apiKey = NAuthState.currentApiKey
        if (apiKey.isBlank()) {
            SnackBar.info("Укажите API-ключ в настройках N, чтобы сохранять альбомы на сервере")
            return
        }
        selectServerFavoriteAccount(apiKey)
        val state = _serverFavorite.value
        if (state.loading || serverFavoriteWrite?.isActive == true) return
        val challenge = state.readError as? CloudflareBlockedException
        val desired = state.desiredUpdate
        when {
            challenge != null -> {
                retryFavorite = ::refreshServerFavorite
                _favoriteChallenge.value = challenge
            }
            desired != null -> setServerFavorite(apiKey, desired)
            else -> refreshServerFavorite()
        }
    }

    private fun setServerFavorite(apiKey: String, save: Boolean) {
        if (serverFavoriteWrite?.isActive == true) return
        val current = _serverFavorite.value
        val version = ++serverFavoriteVersion
        _serverFavorite.value = current.copy(loading = true)
        serverFavoriteWrite = screenModelScope.launch {
            val result = repository.setServerFavorite(galleryId, save, apiKey)
            if (NAuthState.currentApiKey != apiKey) return@launch

            result.fold(
                onSuccess = { saved ->
                    if (version == serverFavoriteVersion) _serverFavorite.value = NServerFavoriteState(saved = saved)
                    SnackBar.success(if (saved) "Альбом добавлен на сервер" else "Альбом удалён с сервера")
                },
                onFailure = { error ->
                    if (version == serverFavoriteVersion) _serverFavorite.value = current
                    if (error is CloudflareBlockedException) {
                        retryFavorite = { if (NAuthState.currentApiKey == apiKey) setServerFavorite(apiKey, save) }
                        _favoriteChallenge.value = error
                    } else {
                        SnackBar.error("Не удалось обновить избранное на сервере: ${error.localizedMessage ?: "ошибка"}")
                    }
                }
            )
        }
    }

    internal fun finishFavoriteChallenge(verified: Boolean) {
        val retry = retryFavorite
        _favoriteChallenge.value = null
        retryFavorite = null
        if (verified) retry?.invoke()
    }

    fun loadGallery() = loadGallery(forceRefresh = true)

    private fun loadGallery(forceRefresh: Boolean) {
        favoriteStatusRefresh.value += 1
        loadJob?.cancel()
        _uiState.value = NGalleryUiState.Loading
        loadJob = screenModelScope.launch {
            repository.getGalleryDetails(galleryId, forceRefresh).fold(
                onSuccess = { gallery ->
                    _uiState.value = NGalleryUiState.Success(gallery)
                },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryUiState.CloudflareRequired(error)
                    } else {
                        _uiState.value = NGalleryUiState.Error(error.localizedMessage ?: "Ошибка загрузки")
                    }
                }
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNGallery {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenNGallerySM.Factory::class)
    abstract fun bindScreenNGallerySMFactory(
        factory: ScreenNGallerySM.Factory
    ): ScreenModelFactory
}

data class NServerFavoriteState(
    val saved: Boolean? = null,
    val loading: Boolean = false,
    val readError: Throwable? = null
) {
    internal val desiredUpdate: Boolean?
        get() = saved?.takeIf { !loading && readError == null }?.not()

    internal fun afterRead(result: Result<Boolean>): NServerFavoriteState = result.fold(
        onSuccess = { NServerFavoriteState(saved = it) },
        onFailure = { NServerFavoriteState(saved = saved, readError = it) }
    )
}

sealed interface NGalleryUiState {
    data object Loading : NGalleryUiState
    data class Success(val gallery: NGallery) : NGalleryUiState
    data class Error(val message: String) : NGalleryUiState
    data class CloudflareRequired(val challenge: CloudflareBlockedException) : NGalleryUiState
}
