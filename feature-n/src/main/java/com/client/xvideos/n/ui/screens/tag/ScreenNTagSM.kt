package com.client.xvideos.n.ui.screens.tag

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScreenModel экрана галерей по выбранному тегу или автору с постраничной пагинацией.
 */
@Stable
class ScreenNTagSM @AssistedInject constructor(
    @Assisted val tag: NTag,
    private val repository: NRepository
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(tag: NTag): ScreenNTagSM
    }

    private val _uiState = MutableStateFlow<NGalleryListUiState>(NGalleryListUiState.Loading)
    val uiState: StateFlow<NGalleryListUiState> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field

    private var loadJob: Job? = null

    init {
        loadPage(1)
    }

    fun loadPage(page: Int) {
        _currentPage.value = page
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            _uiState.value = NGalleryListUiState.Loading
            repository.getGalleriesByTagPage(tag = tag, page = page, sort = NSortType.RECENT).fold(
                onSuccess = { result ->
                    _currentPage.value = result.page
                    _totalPages.value = result.totalPages.coerceAtLeast(1)
                    _uiState.value = NGalleryListUiState.Success(result.items)
                },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryListUiState.CloudflareRequired(error.url)
                    } else {
                        _uiState.value = NGalleryListUiState.Error(
                            error.localizedMessage ?: "Ошибка загрузки тега"
                        )
                    }
                }
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNTag {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenNTagSM.Factory::class)
    abstract fun bindScreenNTagSMFactory(
        factory: ScreenNTagSM.Factory
    ): ScreenModelFactory
}
