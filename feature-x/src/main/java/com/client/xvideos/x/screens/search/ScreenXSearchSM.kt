package com.client.xvideos.x.screens.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateSetOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.common.ui.scroll.PagerPosition
import com.client.xvideos.x.feature.net.getSearchResults
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.search.XSearchHistoryFileStore
import com.client.xvideos.x.search.model.SearchResult
import com.client.xvideos.x.search.parseJson
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Режимы отображения экрана поиска.
 */
enum class SearchUiMode {
    /** Пустой запрос: отображение истории предыдущих поисков. */
    HISTORY,
    /** Активный набор текста: отображение живых подсказок (фраз, авторов, каналов). */
    SUGGESTIONS,
    /** Результаты поиска: пагинированная сетка найденных видеороликов. */
    RESULTS
}

/**
 * ScreenModel вкладки поиска раздела X.
 *
 * Отвечает за автодополнение поисковых фраз, моделей и каналов с дебаунсом,
 * загрузку пагинированных списков видео по запросу, а также хранение истории поиска.
 */
@Stable
class ScreenXSearchSM @Inject constructor(
    val saved: SavedX,
    private val historyStore: XSearchHistoryFileStore
) : ScreenModel {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiMode = MutableStateFlow(SearchUiMode.HISTORY)
    val uiMode: StateFlow<SearchUiMode> = _uiMode.asStateFlow()

    private val _isSuggestLoading = MutableStateFlow(false)
    val isSuggestLoading: StateFlow<Boolean> = _isSuggestLoading.asStateFlow()

    private val _suggestions = MutableStateFlow(SearchResult.EMPTY)
    val suggestions: StateFlow<SearchResult> = _suggestions.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _maxPages = MutableStateFlow(1)
    val maxPages: StateFlow<Int> = _maxPages.asStateFlow()

    /** Загруженные страницы выдачи: номер страницы → ролики. Свайп назад их не перезапрашивает. */
    val resultPages = mutableStateMapOf<Int, List<ItemsX>>()

    /** Страницы выдачи, которые сейчас грузятся. */
    val loadingResultPages = mutableStateSetOf<Int>()

    /** Страницы выдачи, которые не загрузились: на них «Повторить». */
    val failedResultPages = mutableStateSetOf<Int>()

    private val _resultsCount = MutableStateFlow<String?>(null)

    /** Сколько роликов нашлось — из заголовка первой страницы выдачи; `null` — показывать нечего. */
    val resultsCount: StateFlow<String?> = _resultsCount.asStateFlow()

    /**
     * Страница пейджера и позиции сеток выдачи. Живут здесь, а не в композиции:
     * корневой навигатор не хранит состояние ушедшего экрана, и после плеера
     * выдача вставала бы на первую страницу в начало.
     */
    val resultsPagerPosition = PagerPosition()
    private val resultGridPositions = mutableMapOf<Int, LazyGridPosition>()

    fun resultGridPosition(page: Int): LazyGridPosition =
        resultGridPositions.getOrPut(page) { LazyGridPosition() }

    private val resultPageJobs = mutableMapOf<Int, Job>()

    val history: StateFlow<List<String>> = historyStore.observeAllTexts()
        .stateIn(screenModelScope, SharingStarted.Eagerly, emptyList())

    init {
        screenModelScope.launch {
            @OptIn(FlowPreview::class)
            _query
                .debounce(350)
                .collectLatest { rawQuery ->
                    val text = rawQuery.trim()
                    if (text.isBlank()) {
                        _suggestions.value = SearchResult.EMPTY
                        _isSuggestLoading.value = false
                        if (_uiMode.value != SearchUiMode.RESULTS) {
                            _uiMode.value = SearchUiMode.HISTORY
                        }
                        return@collectLatest
                    }

                    if (_uiMode.value == SearchUiMode.RESULTS) {
                        return@collectLatest
                    }

                    _uiMode.value = SearchUiMode.SUGGESTIONS
                    _isSuggestLoading.value = true
                    try {
                        val json = getSearchResults(text)
                        val parsed = parseJson(json) ?: SearchResult.EMPTY
                        _suggestions.value = parsed
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.w("Ошибка запроса подсказок: %s", e.javaClass.simpleName)
                        _suggestions.value = SearchResult.EMPTY
                    } finally {
                        _isSuggestLoading.value = false
                    }
                }
        }
    }

    /**
     * Обновляет текст поискового запроса.
     */
    fun updateQuery(newQuery: String) {
        _query.value = newQuery
        if (_uiMode.value == SearchUiMode.RESULTS) {
            if (newQuery.trim().isEmpty()) {
                _uiMode.value = SearchUiMode.HISTORY
            } else {
                _uiMode.value = SearchUiMode.SUGGESTIONS
            }
        }
    }

    /**
     * Запускает поиск по выбранной фразе или нажатию Enter на клавиатуре.
     */
    fun searchKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return

        _query.value = trimmed
        _uiMode.value = SearchUiMode.RESULTS
        resetResults()

        screenModelScope.launch {
            historyStore.insertAndTrim(trimmed, limit = 30)
        }
        screenModelScope.launch { resultsPagerPosition.scrollToPage(0) }

        loadResultsPage(0)
    }

    /** Новый поиск — с чистой выдачей: страницы, позиции и число найденного прежнего запроса не нужны. */
    private fun resetResults() {
        resultPageJobs.values.forEach { it.cancel() }
        resultPageJobs.clear()
        resultPages.clear()
        loadingResultPages.clear()
        failedResultPages.clear()
        resultGridPositions.clear()
        _currentPage.value = 0
        _maxPages.value = 1
        _resultsCount.value = null
    }

    /**
     * Текущая страница выдачи — от полосы номеров и от свайпа пейджера. Саму
     * страницу загружает пейджер, когда её показывает ([loadResultsPage]).
     */
    fun onPageChange(page: Int) {
        _currentPage.value = page.coerceIn(0, (_maxPages.value - 1).coerceAtLeast(0))
    }

    /** Загружает страницу выдачи, если её ещё нет и она не грузится. */
    fun loadResultsPage(page: Int) {
        val query = _query.value.trim()
        if (query.isEmpty() || page in resultPages || page in loadingResultPages) return
        loadingResultPages.add(page)
        failedResultPages.remove(page)
        // LAZY: задача попадает в resultPageJobs до первого шага, и finally сверяется с ней.
        val job = screenModelScope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext[Job]
            try {
                val result = fetchVideosPage(query, page)
                resultPages[page] = result.items
                // Число страниц и найденного — с первой страницы; дальние могут знать больше страниц.
                if (page == 0) {
                    _maxPages.value = result.maxPages.coerceAtLeast(1)
                    _resultsCount.value = result.totalCount
                } else if (result.maxPages > _maxPages.value) {
                    _maxPages.value = result.maxPages
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e("Ошибка загрузки выдачи поиска (page=%d): %s", page, e.javaClass.simpleName)
                failedResultPages.add(page)
            } finally {
                if (resultPageJobs[page] === self) {
                    resultPageJobs.remove(page)
                    loadingResultPages.remove(page)
                }
            }
        }
        resultPageJobs[page] = job
        job.start()
    }

    /** Повтор страницы выдачи после сбоя. */
    fun retryResultsPage(page: Int) {
        failedResultPages.remove(page)
        loadResultsPage(page)
    }

    private suspend fun fetchVideosPage(query: String, page: Int): com.client.xvideos.x.search.SearchVideosResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return com.client.xvideos.x.search.SearchVideosResult(emptyList(), 1)

        val url = com.client.xvideos.x.search.buildSearchVideosUrl(trimmed, page)
        Timber.d("ScreenXSearchSM.fetchVideosPage: page=%d", page)

        var html = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.client.xvideos.x.feature.net.readHtmlFromURLDirect(url)
        }
        if (html.isBlank()) {
            Timber.w("ScreenXSearchSM: direct HTTP empty, trying WebView: page=%d", page)
            html = com.client.xvideos.x.feature.net.readHtmlFromURLWebView(url)
        }

        if (html.isBlank()) {
            // Оба способа не дали страницы — это сбой, а не «ничего не найдено»:
            // loadVideos покажет ошибку с кнопкой повтора.
            throw java.io.IOException("Не удалось загрузить страницу поиска: $url")
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.client.xvideos.x.search.parseSearchVideosPage(html)
        }
    }

    /**
     * Очищает поле ввода и сбрасывает режим в историю.
     */
    fun clearQuery() {
        _query.value = ""
        _uiMode.value = SearchUiMode.HISTORY
        _suggestions.value = SearchResult.EMPTY
        _isSuggestLoading.value = false
    }

    /**
     * Обработка нажатия системной кнопки «Назад».
     *
     * @return `true` если событие обработано внутри экрана поиска, `false` если передать родителю.
     */
    fun onBackPress(): Boolean {
        return when {
            _uiMode.value == SearchUiMode.RESULTS -> {
                _uiMode.value = if (_query.value.isNotBlank()) SearchUiMode.SUGGESTIONS else SearchUiMode.HISTORY
                true
            }
            _query.value.isNotBlank() -> {
                clearQuery()
                true
            }
            else -> false
        }
    }

    /**
     * Удаляет один элемент из истории поиска.
     */
    fun deleteHistoryItem(text: String) {
        screenModelScope.launch {
            historyStore.deleteByTexts(text)
        }
    }

    /**
     * Очищает всю историю поиска.
     */
    fun clearAllHistory() {
        screenModelScope.launch {
            historyStore.deleteAll()
        }
    }

    fun download(item: ItemsX) = saved.downloads.download(item)
    fun saveToGallery(item: ItemsX) = saved.downloads.saveToGallery(item)
    fun addFavorite(item: ItemsX) = saved.favorites.add(item)
    fun removeFavorite(item: ItemsX) = saved.favorites.remove(item)
    fun isFavorite(id: Long): Boolean = saved.favorites.contains(id)
}

/**
 * Hilt-модуль биндинга [ScreenXSearchSM].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleSearch {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenXSearchSM::class)
    abstract fun bindScreenXSearchSM(hiltModel: ScreenXSearchSM): ScreenModel
}
