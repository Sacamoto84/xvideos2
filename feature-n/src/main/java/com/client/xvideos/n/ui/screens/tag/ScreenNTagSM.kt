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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScreenModel экрана галерей по выбранному тегу или автору.
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

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    var currentPage = 1
        private set
    var isEndReached = false
        private set

    val columns = Settings.n_galleriesTab_column_current_count.field.value

    val tagQuery: String = when {
        tag.type.isNotEmpty() && tag.type != "tag" -> "${tag.type}:\"${tag.name}\""
        else -> "\"${tag.name}\""
    }

    init {
        loadTagGalleries()
    }

    fun loadTagGalleries() {
        currentPage = 1
        isEndReached = false
        _isLoadingMore.value = false

        screenModelScope.launch {
            _uiState.value = NGalleryListUiState.Loading
            repository.search(query = tagQuery, page = 1, sort = NSortType.RECENT).fold(
                onSuccess = { galleries ->
                    _uiState.value = NGalleryListUiState.Success(galleries)
                },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryListUiState.CloudflareRequired(error.url)
                    } else {
                        _uiState.value = NGalleryListUiState.Error(error.localizedMessage ?: "Ошибка загрузки тега")
                    }
                }
            )
        }
    }

    fun loadNextPage() {
        val currentItems = (_uiState.value as? NGalleryListUiState.Success)?.items ?: return
        if (_isLoadingMore.value || isEndReached) return

        _isLoadingMore.value = true
        val nextPage = currentPage + 1

        screenModelScope.launch {
            repository.search(query = tagQuery, page = nextPage, sort = NSortType.RECENT).fold(
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
abstract class ScreenModuleNTag {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenNTagSM.Factory::class)
    abstract fun bindScreenNTagSMFactory(
        factory: ScreenNTagSM.Factory
    ): ScreenModelFactory
}
