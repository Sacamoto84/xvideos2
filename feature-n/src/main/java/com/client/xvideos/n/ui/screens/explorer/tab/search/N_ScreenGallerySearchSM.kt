package com.client.xvideos.n.ui.screens.explorer.tab.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NAlbumFilter
import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.model.NSortType
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

    var query by mutableStateOf(TextFieldValue())
        private set

    private val _tagSuggestions = MutableStateFlow<List<NTag>>(emptyList())
    val tagSuggestions: StateFlow<List<NTag>> = _tagSuggestions.asStateFlow()

    private val _filter = MutableStateFlow(NAlbumFilter.DEFAULT)
    val filter: StateFlow<NAlbumFilter> = _filter.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field

    private var tagSearchJob: Job? = null
    private var submittedSearch by mutableStateOf(NSubmittedSearch("", NSortType.RECENT))
    val hasQuery: Boolean get() = submittedSearch.isActive
    val pages = NGalleryPages(screenModelScope) { number ->
        submittedSearch.loadPage(repository, number)
    }

    suspend fun loadTagPage(type: NTagType, sort: NTagSort, page: Int) =
        repository.getTagsPage(type, sort, page)

    suspend fun findTags(query: String, type: NTagType) =
        repository.searchTags(query, limit = 50, type = type)

    fun updateQuery(newQuery: TextFieldValue) {
        val previous = query
        query = newQuery
        if (previous.text == newQuery.text && previous.selection == newQuery.selection) return
        tagSearchJob?.cancel()
        _tagSuggestions.value = emptyList()
        if (!newQuery.selection.collapsed) return
        val token = activeNTagToken(newQuery.text, newQuery.selection.start)
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
        if (tag !in _tagSuggestions.value) return
        query = replaceActiveNTagToken(query, tag)
        search()
    }

    fun search() {
        tagSearchJob?.cancel()
        _tagSuggestions.value = emptyList()
        val filter = _filter.value
        submittedSearch = NSubmittedSearch(filter.buildQuery(query.text), filter.sort)
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

internal data class NSubmittedSearch(private val query: String, private val sort: NSortType) {
    val isActive get() = query.isNotEmpty() || sort != NSortType.RECENT

    suspend fun loadPage(repository: NRepository, page: Int): Result<NGalleryPageResult> = when {
        query.isNotEmpty() -> repository.searchPage(query, page, sort)
        isActive -> repository.getGalleriesPage(page, sort)
        else -> Result.success(NGalleryPageResult())
    }
}

internal data class NActiveTagToken(
    val start: Int,
    val endExclusive: Int,
    val searchTerm: String,
    val type: NTagType?,
    val excluded: Boolean
)

private val escapedTagCharacter = Regex("""\\(["\\])""")

internal fun activeNTagToken(query: String, caret: Int = query.length): NActiveTagToken {
    var start = 0
    var end = query.length
    var quoted = false
    var quotedAtCaret = false
    var escaped = false
    for ((index, char) in query.withIndex()) {
        if (index == caret) quotedAtCaret = quoted
        when {
            escaped -> escaped = false
            char == '\\' && quoted -> escaped = true
            char == '"' -> quoted = !quoted
            !quoted && (char.isWhitespace() || char == '(' || char == ')') -> {
                if (index >= caret) {
                    end = index
                    break
                }
                start = index + 1
            }
        }
    }
    if (caret == query.length) quotedAtCaret = quoted
    val raw = query.substring(start, caret)
    val token = raw.removePrefix("-")
    val type = NTagType.entries.firstOrNull { token.startsWith(it.value + ":", ignoreCase = true) }
    val name = if (type == null) token else token.substringAfter(':')
    val unquotedName = if (name.startsWith('"')) {
        val content = name.drop(1)
        if (quotedAtCaret) content else content.removeSuffix("\"")
    } else name
    val searchTerm = escapedTagCharacter.replace(unquotedName) { it.groupValues[1] }
    return NActiveTagToken(start, end, searchTerm, type, excluded = raw.startsWith('-'))
}

internal fun replaceActiveNTagToken(query: TextFieldValue, tag: NTag): TextFieldValue {
    val token = activeNTagToken(query.text, query.selection.start)
    val replacement = NTagCondition(tag, excluded = token.excluded).searchToken
    return TextFieldValue(
        text = query.text.replaceRange(token.start, token.endExclusive, replacement),
        selection = TextRange(token.start + replacement.length)
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_ScreenGallerySearchModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGallerySearchSM::class)
    abstract fun bindN_ScreenGallerySearchSM(sm: N_ScreenGallerySearchSM): ScreenModel
}
