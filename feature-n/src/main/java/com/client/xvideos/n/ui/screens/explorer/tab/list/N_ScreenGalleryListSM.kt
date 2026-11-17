package com.client.xvideos.n.ui.screens.explorer.tab.list

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.repository.NRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ScreenModel вкладки со списком свежей манги.
 */
@Stable
class N_ScreenGalleryListSM @Inject constructor(
    private val repository: NRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow<NGalleryListUiState>(NGalleryListUiState.Loading)
    val uiState: StateFlow<NGalleryListUiState> = _uiState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    var currentPage = 1
        private set
    var isEndReached = false
        private set

    val columns = Settings.n_galleriesTab_column_current_count.field

    private var loadJob: Job? = null

    init {
        loadGalleries()
    }

    fun loadGalleries() {
        currentPage = 1
        isEndReached = false
        _isLoadingMore.value = false
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            _uiState.value = NGalleryListUiState.Loading
            repository.getGalleries(page = 1, sort = NSortType.RECENT).fold(
                onSuccess = { galleries ->
                    _uiState.value = NGalleryListUiState.Success(galleries)
                },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryListUiState.CloudflareRequired(error.url)
                    } else {
                        _uiState.value = NGalleryListUiState.Error(error.localizedMessage ?: "Ошибка загрузки ленты")
                    }
                }
            )
        }
    }

    /**
     * Подгружает следующую страницу ленты манги.
     */
    fun loadNextPage() {
        val currentItems = (_uiState.value as? NGalleryListUiState.Success)?.items ?: return
        if (_isLoadingMore.value || isEndReached) return

        _isLoadingMore.value = true
        val nextPage = currentPage + 1

        screenModelScope.launch {
            repository.getGalleries(page = nextPage, sort = NSortType.RECENT).fold(
                onSuccess = { newGalleries ->
                    if (newGalleries.isEmpty()) {
                        isEndReached = true
                    } else {
                        currentPage = nextPage
                        _uiState.value = NGalleryListUiState.Success(currentItems + newGalleries)
                    }
                    _isLoadingMore.value = false
                },
                onFailure = {
                    _isLoadingMore.value = false
                }
            )
        }
    }

    /**
     * Запрашивает ID случайной манги и передает его в колбэк навигации.
     */
    fun openRandomGallery(onSuccess: (Long) -> Unit) {
        screenModelScope.launch {
            repository.getRandomGalleryId().fold(
                onSuccess = { id -> onSuccess(id) },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryListUiState.CloudflareRequired(error.url)
                    }
                }
            )
        }
    }
}

sealed interface NGalleryListUiState {
    data object Loading : NGalleryListUiState
    data class Success(val items: List<NGalleryPreview>) : NGalleryListUiState
    data class Error(val message: String) : NGalleryListUiState
    data class CloudflareRequired(val url: String) : NGalleryListUiState
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_ScreenGalleryListModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGalleryListSM::class)
    abstract fun bindN_ScreenGalleryListSM(sm: N_ScreenGalleryListSM): ScreenModel
}
