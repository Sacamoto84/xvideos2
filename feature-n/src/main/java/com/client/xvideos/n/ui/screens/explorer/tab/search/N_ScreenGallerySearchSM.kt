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
import kotlinx.coroutines.ensureActive
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
        _tagSuggestions.value = emptyList()
        val token = activeNTagToken(newQuery)
        if (token.searchTerm.length >= 2) {
            tagSearchJob = screenModelScope.launch {
                delay(300)
                val result = repository.searchTags(token.searchTerm, limit = 8, type = token.type)
                ensureActive()
                _tagSuggestions.value = result.getOrDefault(emptyList())
            }
        }
    }

    /**
     * Подставляет выбранный тег из подсказок и запускает поиск.
     */
    fun selectTagSuggestion(tag: NTag) {
        val current = _query.value
        val token = activeNTagToken(current)
        _query.value = current.take(token.start) + NTagCondition(tag, excluded = token.excluded).searchToken
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

internal data class NActiveTagToken(
    val start: Int,
    val searchTerm: String,
    val type: NTagType?,
    val excluded: Boolean
)

private val escapedTagCharacter = Regex("""\\(["\\])""")

internal fun activeNTagToken(query: String): NActiveTagToken {
    var start = 0
    var quoted = false
    var escaped = false
    for ((index, char) in query.withIndex()) {
        when {
            escaped -> escaped = false
            char == '\\' && quoted -> escaped = true
            char == '"' -> quoted = !quoted
            char.isWhitespace() && !quoted -> start = index + 1
        }
    }
    val raw = query.substring(start)
    val token = raw.removePrefix("-")
    val type = NTagType.entries.firstOrNull { token.startsWith(it.value + ":", ignoreCase = true) }
    val name = if (type == null) token else token.substringAfter(':')
    val unquotedName = if (name.startsWith('"')) {
        val content = name.drop(1)
        if (quoted) content else content.removeSuffix("\"")
    } else name
    val searchTerm = escapedTagCharacter.replace(unquotedName) { it.groupValues[1] }
    return NActiveTagToken(start, searchTerm, type, excluded = raw.startsWith('-'))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_ScreenGallerySearchModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGallerySearchSM::class)
    abstract fun bindN_ScreenGallerySearchSM(sm: N_ScreenGallerySearchSM): ScreenModel
}
