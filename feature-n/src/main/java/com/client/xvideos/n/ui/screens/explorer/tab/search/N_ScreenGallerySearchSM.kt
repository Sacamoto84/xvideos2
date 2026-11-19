package com.client.xvideos.n.ui.screens.explorer.tab.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NAlbumFilter
import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.model.NTagCondition
import com.client.xvideos.n.model.NTagSort
import com.client.xvideos.n.model.NTagType
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryPages
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _filter = MutableStateFlow(NAlbumFilter.DEFAULT)
    val filter: StateFlow<NAlbumFilter> = _filter.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field

    private var tagSearchJob: Job? = null
    private var submittedQuery by mutableStateOf("")
    val hasQuery: Boolean get() = submittedQuery.isNotEmpty()
    val pages = NGalleryPages(screenModelScope) { number ->
        if (submittedQuery.isEmpty()) Result.success(NGalleryPageResult())
        else repository.searchPage(query = submittedQuery, page = number, sort = _filter.value.sort)
    }

    suspend fun loadTagPage(type: NTagType, sort: NTagSort, page: Int) =
        repository.getTagsPage(type, sort, page)

    suspend fun findTags(query: String, type: NTagType) =
        repository.searchTags(query, limit = 50, type = type)

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
        tagSearchJob?.cancel()
        _tagSuggestions.value = emptyList()
        submittedQuery = _filter.value.buildQuery(_query.value)
        pages.reset()
    }

    fun setFilter(newFilter: NAlbumFilter) {
        _filter.value = newFilter
        search()
    }

    fun removeTagCondition(condition: NTagCondition) {
        setFilter(_filter.value.copy(tags = _filter.value.tags - condition))
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
