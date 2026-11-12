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

    val columns = Settings.n_galleriesTab_column_current_count.field

    init {
        loadGalleries()
    }

    fun loadGalleries() {
        screenModelScope.launch {
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
