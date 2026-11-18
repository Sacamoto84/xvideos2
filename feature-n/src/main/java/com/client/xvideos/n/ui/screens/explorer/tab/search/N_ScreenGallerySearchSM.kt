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
 * ScreenModel вкладки поиска манги с постраничной пагинацией.
 */
@Stable
class N_ScreenGallerySearchSM @Inject constructor(
    private val repository: NRepository
) : ScreenModel {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _tagSuggestions = MutableStateFlow<List<NTag>>(emptyList())
    val tagSuggestions: StateFlow<List<NTag>> = _tagSuggestions.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _uiState = MutableStateFlow<NGalleryListUiState>(NGalleryListUiState.Success(emptyList()))
    val uiState: StateFlow<NGalleryListUiState> = _uiState.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field

    private var tagSearchJob: Job? = null
    private var searchJob: Job? = null

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
        loadPage(1)
    }

    fun loadPage(page: Int) {
        val q = _query.value.trim()
        if (q.isEmpty()) return

        tagSearchJob?.cancel()
        searchJob?.cancel()
        _tagSuggestions.value = emptyList()
        _currentPage.value = page

        searchJob = screenModelScope.launch {
            _uiState.value = NGalleryListUiState.Loading
            repository.searchPage(query = q, page = page, sort = NSortType.RECENT).fold(
                onSuccess = { result ->
                    _currentPage.value = result.page
                    _totalPages.value = result.totalPages.coerceAtLeast(1)
                    _uiState.value = NGalleryListUiState.Success(result.items)
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
