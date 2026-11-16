package com.client.xvideos.n.ui.screens.explorer.tab.tophits

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ScreenModel вкладки популярной манги (Top Hits).
 */
@Stable
class N_ScreenGalleryTopHitsSM @Inject constructor(
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

    private val _sortType = MutableStateFlow(NSortType.POPULAR_ALL_TIME)
    val sortType: StateFlow<NSortType> = _sortType.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field

    init {
        loadTopHits()
    }

    fun setSortType(sort: NSortType) {
        _sortType.value = sort
        loadTopHits()
    }

    fun loadTopHits() {
        currentPage = 1
        isEndReached = false
        _isLoadingMore.value = false
        screenModelScope.launch {
            _uiState.value = NGalleryListUiState.Loading
            repository.getGalleries(page = 1, sort = _sortType.value).fold(
                onSuccess = { galleries ->
                    _uiState.value = NGalleryListUiState.Success(galleries)
                },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryListUiState.CloudflareRequired(error.url)
                    } else {
                        _uiState.value = NGalleryListUiState.Error(error.localizedMessage ?: "Ошибка загрузки топа")
                    }
                }
            )
        }
    }

    /**
     * Загружает следующую страницу популярной манги.
     */
    fun loadNextPage() {
        val currentItems = (_uiState.value as? NGalleryListUiState.Success)?.items ?: return
        if (_isLoadingMore.value || isEndReached) return

        _isLoadingMore.value = true
        val nextPage = currentPage + 1

        screenModelScope.launch {
            repository.getGalleries(page = nextPage, sort = _sortType.value).fold(
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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_ScreenGalleryTopHitsModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGalleryTopHitsSM::class)
    abstract fun bindN_ScreenGalleryTopHitsSM(sm: N_ScreenGalleryTopHitsSM): ScreenModel
}
