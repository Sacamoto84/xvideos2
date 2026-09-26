package com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.selectionKey
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
 * [ScreenModel] экрана серверных лайков картинок пользователя Luscious.
 *
 * Управляет постраничной подгрузкой лайкнутых картинок через GraphQL API Luscious,
 * обновлением состояния, удалением лайков и интеграцией с [LazyRowPictureDetailsHost].
 *
 * @param repository Репозиторий доступа к серверным подпискам и избранному.
 */
@Stable
class ScreenLServerLikesSM @Inject constructor(
    private val repository: LusciousServerFavoritesRepository
) : ScreenModel {

    /** Хост состояния сетки и выбора картинок. */
    val host = LazyRowPictureDetailsHost("l_server_likes")

    private val _pictures = MutableStateFlow<List<PicsDetails>>(emptyList())
    /** Поток списка картинок, понравившихся пользователю на сервере. */
    val pictures = _pictures.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** Поток флага фоновой загрузки (первой или следующей страницы). */
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    /** Поток флага обновления списка с первой страницы (pull-to-refresh). */
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** Поток текста ошибки загрузки либо null при успешной работе. */
    val errorMessage = _errorMessage.asStateFlow()

    /** Флаг наличия доступных следующих страниц для пагинации. */
    var hasMore: Boolean = true
        private set

    private var currentPage: Int = 1
    private var loadJob: Job? = null

    init {
        host.onItemRemoved = { removedPic ->
            val targetKey = removedPic.selectionKey()
            _pictures.value = _pictures.value.filterNot {
                (!it.id.isNullOrBlank() && it.id == removedPic.id) ||
                    it.selectionKey() == targetKey
            }
        }
        loadInitial()
    }

    /**
     * Удаляет картинку из локального списка хоста после снятия лайка на сервере.
     *
     * @param pic Картинка, лайк с которой был снят.
     */
    fun unlikePicture(pic: PicsDetails) {
        host.removePicture(pic)
    }

    /**
     * Загружает начальную первую страницу серверных лайков.
     */
    fun loadInitial() {
        if (_isLoading.value) return
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 1
            val result = repository.getServerLikedPictures(currentPage)
            result.onSuccess { list ->
                _pictures.value = list
                host.replaceFilteredPictures(list)
                hasMore = list.isNotEmpty()
                _errorMessage.value = null
            }.onFailure { error ->
                Timber.e(error, "Failed to load server liked pictures")
                _errorMessage.value = error.message ?: "Ошибка загрузки лайков с сервера"
            }
            _isLoading.value = false
        }
    }

    /**
     * Загружает следующую страницу серверных лайков для бесконечного скролла.
     */
    fun loadNextPage() {
        if (_isLoading.value || !hasMore || _errorMessage.value != null) return
        loadJob = screenModelScope.launch {
            _isLoading.value = true
            val nextPage = currentPage + 1
            val result = repository.getServerLikedPictures(nextPage)
            result.onSuccess { list ->
                if (list.isNotEmpty()) {
                    currentPage = nextPage
                    val combined = _pictures.value + list
                    _pictures.value = combined
                    host.replaceFilteredPictures(combined)
                } else {
                    hasMore = false
                }
            }.onFailure { error ->
                Timber.e(error, "Failed to load next page ($nextPage) of server likes")
            }
            _isLoading.value = false
        }
    }

    /**
     * Принудительно перезагружает список лайков с первой страницы.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        loadJob?.cancel()
        _isLoading.value = false
        screenModelScope.launch {
            _isRefreshing.value = true
            currentPage = 1
            val result = repository.getServerLikedPictures(currentPage)
            result.onSuccess { list ->
                _pictures.value = list
                host.replaceFilteredPictures(list)
                hasMore = list.isNotEmpty()
                _errorMessage.value = null
            }.onFailure { error ->
                Timber.e(error, "Failed to refresh server likes")
                _errorMessage.value = error.message ?: "Ошибка обновления лайков с сервера"
            }
            _isRefreshing.value = false
        }
    }
}

/**
 * Hilt-модуль мультибиндинга для [ScreenLServerLikesSM].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLServerLikes {

    /** Регистрирует [ScreenLServerLikesSM] в карте ScreenModel Voyager. */
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLServerLikesSM::class)
    abstract fun bindScreenLServerLikesSM(sm: ScreenLServerLikesSM): ScreenModel
}
