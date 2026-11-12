package com.client.xvideos.n.ui.screens.explorer.tab.search

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
 * ScreenModel вкладки поиска манги.
 */
@Stable
class N_ScreenGallerySearchSM @Inject constructor(
    private val repository: NRepository
) : ScreenModel {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<NGalleryListUiState>(NGalleryListUiState.Success(emptyList()))
    val uiState: StateFlow<NGalleryListUiState> = _uiState.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field

    fun setQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return

        screenModelScope.launch {
            _uiState.value = NGalleryListUiState.Loading
            repository.search(query = q, page = 1, sort = NSortType.RECENT).fold(
                onSuccess = { galleries ->
                    _uiState.value = NGalleryListUiState.Success(galleries)
                },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryListUiState.CloudflareRequired(error.url)
                    } else {
                        _uiState.value = NGalleryListUiState.Error(error.localizedMessage ?: "Ошибка поиска")
                    }
                }
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_ScreenGallerySearchModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGallerySearchSM::class)
    abstract fun bindN_ScreenGallerySearchSM(sm: N_ScreenGallerySearchSM): ScreenModel
}
