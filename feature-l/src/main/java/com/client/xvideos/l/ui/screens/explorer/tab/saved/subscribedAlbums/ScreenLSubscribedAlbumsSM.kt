package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
class ScreenLSubscribedAlbumsSM @Inject constructor(
    private val repository: LusciousServerFavoritesRepository
) : ScreenModel {

    val state = LazyGridState()

    private val _albums = MutableStateFlow<List<AlbumDetails>>(emptyList())
    val albums = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    var hasMore: Boolean = true
        private set

    private var currentPage: Int = 1
    private var loadJob: Job? = null

    init {
        loadInitial()
    }

    fun loadInitial() {
        if (_isLoading.value) return
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 1

            val result = repository.getSubscribedAlbums(currentPage)
            result.onSuccess { list ->
                _albums.value = list
                hasMore = list.isNotEmpty()
                _errorMessage.value = null
            }.onFailure { error ->
                Timber.e(error, "Failed to load subscribed albums from session")
                _errorMessage.value = error.message ?: "Ошибка загрузки подписок"
            }
            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || !hasMore || _errorMessage.value != null) return
        loadJob = screenModelScope.launch {
            _isLoading.value = true
            val nextPage = currentPage + 1
            val result = repository.getSubscribedAlbums(nextPage)
            result.onSuccess { list ->
                if (list.isNotEmpty()) {
                    currentPage = nextPage
                    _albums.value = _albums.value + list
                } else {
                    hasMore = false
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to load next page ($nextPage) of subscribed albums")
            }
            _isLoading.value = false
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        loadJob?.cancel()
        _isLoading.value = false
        screenModelScope.launch {
            _isRefreshing.value = true
            currentPage = 1
            val result = repository.getSubscribedAlbums(currentPage)
            result.onSuccess { list ->
                _albums.value = list
                hasMore = list.isNotEmpty()
                _errorMessage.value = null
            }.onFailure { error ->
                Timber.e(error, "Failed to refresh subscribed albums")
                _errorMessage.value = error.message ?: "Ошибка обновления подписок"
            }
            _isRefreshing.value = false
        }
    }

    fun unlikeAlbum(album: AlbumDetails) {
        screenModelScope.launch {
            val result = repository.unlikeAlbum(album.id)
            result.onSuccess {
                _albums.value = _albums.value.filter { it.id != album.id }
                SnackBar.info("Альбом удалён из подписок")
            }.onFailure { error ->
                Timber.e(error, "Failed to unsubscribe album ${album.id} on server")
                SnackBar.error(error.message ?: "Не удалось удалить альбом из подписок")
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSubscribedAlbums {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLSubscribedAlbumsSM::class)
    abstract fun bindScreenLSubscribedAlbumsSM(sm: ScreenLSubscribedAlbumsSM): ScreenModel
}
