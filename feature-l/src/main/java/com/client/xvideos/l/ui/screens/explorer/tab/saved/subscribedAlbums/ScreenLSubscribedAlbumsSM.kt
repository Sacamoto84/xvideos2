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

/**
 * [ScreenModel] экрана подписанных (избранных на сервере) альбомов пользователя Luscious.
 *
 * Осуществляет пагинацию серверного списка подписок, отслеживает состояние загрузки
 * и предоставляет возможность удаления альбома из подписок.
 *
 * @param repository Репозиторий доступа к серверным подпискам и избранному.
 */
@Stable
class ScreenLSubscribedAlbumsSM @Inject constructor(
    private val repository: LusciousServerFavoritesRepository
) : ScreenModel {

    /** Состояние прокрутки сетки подписанных альбомов. */
    val state = LazyGridState()

    private val _albums = MutableStateFlow<List<AlbumDetails>>(emptyList())
    /** Поток списка подписанных альбомов пользователя. */
    val albums = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** Поток индикатора первичной загрузки или пагинации. */
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    /** Поток индикатора обновления списка (pull-to-refresh). */
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** Поток текста ошибки загрузки. */
    val errorMessage = _errorMessage.asStateFlow()

    /** Флаг наличия последующих страниц для загрузки. */
    var hasMore: Boolean = true
        private set

    private var currentPage: Int = 1
    private var loadJob: Job? = null

    init {
        loadInitial()
    }

    /**
     * Загружает начальную первую страницу подписанных альбомов.
     */
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

    /**
     * Загружает следующую страницу альбомов для бесконечной ленты.
     */
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

    /**
     * Обновляет список подписок с первой страницы.
     */
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

    /**
     * Отменяет подписку на альбом [album] на сервере и удаляет его из локального списка.
     *
     * @param album Альбом, подписку на который необходимо удалить.
     */
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

/**
 * Hilt-модуль мультибиндинга для [ScreenLSubscribedAlbumsSM].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSubscribedAlbums {

    /** Регистрирует [ScreenLSubscribedAlbumsSM] в карте ScreenModel Voyager. */
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLSubscribedAlbumsSM::class)
    abstract fun bindScreenLSubscribedAlbumsSM(sm: ScreenLSubscribedAlbumsSM): ScreenModel
}
