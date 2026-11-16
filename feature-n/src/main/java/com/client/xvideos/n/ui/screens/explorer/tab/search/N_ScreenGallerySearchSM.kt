package com.client.xvideos.n.ui.screens.explorer.tab.search

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _tagSuggestions = MutableStateFlow<List<NTag>>(emptyList())
    val tagSuggestions: StateFlow<List<NTag>> = _tagSuggestions.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    var currentPage = 1
        private set
    var isEndReached = false
        private set

    private val _uiState = MutableStateFlow<NGalleryListUiState>(NGalleryListUiState.Success(emptyList()))
    val uiState: StateFlow<NGalleryListUiState> = _uiState.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field

    private var tagSearchJob: Job? = null

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        tagSearchJob?.cancel()

        val trimmed = newQuery.trim()
        val lastWord = trimmed.substringAfterLast(' ')
        if (lastWord.length >= 2) {
            tagSearchJob = screenModelScope.launch {
                delay(300)
                val cleanWord = lastWord.removePrefix("tag:").removePrefix("artist:").removeSurrounding("\"")
                if (cleanWord.length >= 2) {
                    val result = repository.searchTags(cleanWord, limit = 8)
                    _tagSuggestions.value = result.getOrDefault(emptyList())
                } else {
                    _tagSuggestions.value = emptyList()
                }
            }
        } else {
            _tagSuggestions.value = emptyList()
        }
    }

    /**
     * Подставляет выбранный тег из подсказок и запускает поиск.
     */
    fun selectTagSuggestion(tag: NTag) {
        val current = _query.value.trim()
        val tagToken = if (tag.type == "tag" || tag.type.isEmpty()) {
            "\"${tag.name}\""
        } else {
            "${tag.type}:\"${tag.name}\""
        }

        val updated = if (current.contains(' ')) {
            current.substringBeforeLast(' ') + " " + tagToken
        } else {
            tagToken
        }

        _query.value = updated
        _tagSuggestions.value = emptyList()
        search()
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return

        tagSearchJob?.cancel()
        _tagSuggestions.value = emptyList()
        currentPage = 1
        isEndReached = false
        _isLoadingMore.value = false

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

    /**
     * Загружает следующую страницу результатов поиска.
     */
    fun loadNextPage() {
        val q = _query.value.trim()
        val currentItems = (_uiState.value as? NGalleryListUiState.Success)?.items ?: return
        if (q.isEmpty() || _isLoadingMore.value || isEndReached) return

        _isLoadingMore.value = true
        val nextPage = currentPage + 1

        screenModelScope.launch {
            repository.search(query = q, page = nextPage, sort = NSortType.RECENT).fold(
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
abstract class N_ScreenGallerySearchModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGallerySearchSM::class)
    abstract fun bindN_ScreenGallerySearchSM(sm: N_ScreenGallerySearchSM): ScreenModel
}
