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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ScreenModel вкладки популярной манги (Top Hits) с постраничной пагинацией.
 */
@Stable
class N_ScreenGalleryTopHitsSM @Inject constructor(
    private val repository: NRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow<NGalleryListUiState>(NGalleryListUiState.Loading)
    val uiState: StateFlow<NGalleryListUiState> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _sortType = MutableStateFlow(NSortType.POPULAR_ALL_TIME)
    val sortType: StateFlow<NSortType> = _sortType.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field

    private var loadJob: Job? = null

    init {
        loadPage(1)
    }

    fun setSortType(sort: NSortType) {
        _sortType.value = sort
        loadPage(1)
    }

    fun loadPage(page: Int) {
        _currentPage.value = page
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            _uiState.value = NGalleryListUiState.Loading
            repository.getGalleriesPage(page = page, sort = _sortType.value).fold(
                onSuccess = { result ->
                    _currentPage.value = result.page
                    _totalPages.value = result.totalPages.coerceAtLeast(1)
                    _uiState.value = NGalleryListUiState.Success(result.items)
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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNGalleryTopHits {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGalleryTopHitsSM::class)
    abstract fun bindN_ScreenGalleryTopHitsSM(model: N_ScreenGalleryTopHitsSM): ScreenModel
}
