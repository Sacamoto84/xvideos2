package com.client.xvideos.xlive.ui.tags

import com.client.xvideos.common.ui.scroll.LazyListPosition
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.network.TAG_PAGE_SIZE
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

/**
 * Таблица тегов постранично: страницы с единицы по [TAG_PAGE_SIZE], без
 * повторов по имени — порядок по зрителям живой, теги между страницами
 * переезжают.
 *
 * Отбор (пол, порядок) знает тот, кто держит таблицу: [loadPage] берёт его сам.
 * Имена тегов в журнал не идут: теги — слова из тем комнат.
 *
 * @param loadPage страница с номером под текущий отбор.
 * @param label отбор для журнала.
 */
class XLiveTagsFeed(
    private val scope: CoroutineScope,
    private val loadPage: suspend (page: Int) -> XLiveTagPage,
    private val label: () -> String,
) {

    private val _state = MutableStateFlow(XLiveTagsState())
    val state: StateFlow<XLiveTagsState> = _state.asStateFlow()

    /** Позиция списка; живёт у таблицы, а не в композиции — как у сетки комнат. */
    val listPosition = LazyListPosition()

    /** Таблица хоть раз запрошена: до первого открытия вкладки сеть не трогаем. */
    var isStarted: Boolean = false
        private set

    private var loadJob: Job? = null
    private var nextPage = 1

    /** Первое открытие вкладки — первая страница; дальше ничего. */
    fun start() {
        if (!isStarted) reload()
    }

    /** Таблица с нуля: смена отбора и «Повторить». */
    fun reload() {
        isStarted = true
        loadJob?.cancel()
        _state.value = XLiveTagsState(isLoading = true)
        scope.launch { listPosition.scrollToItem(0) }
        loadJob = scope.launch {
            try {
                val page = loadPage(1)
                logPage(1, page)
                nextPage = 2
                _state.value = XLiveTagsState(
                    tags = page.tags.distinctBy { it.name }.toImmutableList(),
                    isEndReached = isLast(page, loadedPages = 1),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w("XLive: теги не получены — %s", e.javaClass.simpleName)
                _state.value = XLiveTagsState(error = "Не удалось загрузить теги")
            }
        }
    }

    /** Следующая страница, когда список докрутился до конца. */
    fun loadMore() {
        val current = _state.value
        val busy = current.isLoading || current.isLoadingMore
        val blocked = current.tags.isEmpty() || current.isEndReached || current.loadMoreError != null
        if (busy || blocked) return
        _state.update { it.copy(isLoadingMore = true) }
        val pageNumber = nextPage
        loadJob = scope.launch {
            try {
                val page = loadPage(pageNumber)
                logPage(pageNumber, page)
                nextPage = pageNumber + 1
                _state.update { state ->
                    state.copy(
                        tags = (state.tags + page.tags).distinctBy { it.name }.toImmutableList(),
                        isLoadingMore = false,
                        isEndReached = isLast(page, loadedPages = pageNumber),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w("XLive: страница тегов не получена — %s", e.javaClass.simpleName)
                _state.update { it.copy(isLoadingMore = false, loadMoreError = "Не удалось загрузить ещё") }
            }
        }
    }

    fun retryLoadMore() {
        _state.update { it.copy(loadMoreError = null) }
        loadMore()
    }

    private fun logPage(number: Int, page: XLiveTagPage) {
        Timber.i("XLive: теги %s, страница %d — тегов %d из %d", label(), number, page.tags.size, page.total)
    }

    private fun isLast(page: XLiveTagPage, loadedPages: Int): Boolean =
        page.tags.isEmpty() || loadedPages * TAG_PAGE_SIZE >= page.total
}
