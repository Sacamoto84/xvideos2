package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.client.xvideos.r.model.GifsInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * Лента с фильтром блок-листа глазами экрана.
 *
 * Сетка ленты уходит из композиции при каждом переходе на другую вкладку или
 * экран, а её состояние прокрутки живёт дольше. Вернувшись, экран обязан
 * получить загруженные страницы в первом же кадре: с пустым списком сетка
 * сбрасывает позицию на начало. `collectAsLazyPagingItems` берёт такие страницы
 * только из `replayCache` потока, поэтому фильтр, поставленный после кэша,
 * сбрасывал прокрутку сетевых лент при каждом возврате.
 */
class LazyRow123BlockedFeedTest {

    @Test
    fun `вернувшись к ленте, экран сразу получает загруженные страницы`() = feedTest { main, scope ->
        val feed = pagesOf("a", "b", "c").cachedIn(scope).withoutBlocked(MutableStateFlow(setOf("b")), scope)

        val firstVisit = FeedPresenter(main, cached = null)
        val collecting = launch { feed.collectLatest { firstVisit.collectFrom(it) } }
        firstVisit.awaitIds("a", "c")
        collecting.cancel()

        val secondVisit = FeedPresenter(main, cached = feed.cachedForNewScreen())

        assertEquals(listOf("a", "c"), secondVisit.ids)
    }

    @Test
    fun `ролик, заблокированный при открытой ленте, пропадает без запроса к источнику`() = feedTest { main, scope ->
        val source = CountingPages("a", "b", "c")
        val blocked = MutableStateFlow(emptySet<String>())
        val feed = source.flow.cachedIn(scope).withoutBlocked(blocked, scope)

        val screen = FeedPresenter(main, cached = null)
        val collecting = launch { feed.collectLatest { screen.collectFrom(it) } }
        screen.awaitIds("a", "b", "c")

        blocked.value = setOf("b")
        screen.awaitIds("a", "c")
        collecting.cancel()

        assertEquals("лента перезапросила источник", 1, source.loads)
        assertEquals(listOf("a", "c"), FeedPresenter(main, cached = feed.cachedForNewScreen()).ids)
    }

    /** Всё на одном потоке `runBlocking`: и кэш ленты, и «экран». */
    private fun feedTest(
        body: suspend CoroutineScope.(main: CoroutineContext, scope: CoroutineScope) -> Unit,
    ) = runBlocking {
        val main = coroutineContext[ContinuationInterceptor]!!
        val scope = CoroutineScope(main + Job())
        try {
            body(main, scope)
        } finally {
            scope.cancel()
        }
    }

    private fun pagesOf(vararg ids: String): Flow<PagingData<GifsInfo>> = CountingPages(*ids).flow

    private class CountingPages(vararg ids: String) {
        private val items = ids.map { GifsInfo(id = it) }
        var loads = 0
            private set

        val flow: Flow<PagingData<GifsInfo>> = Pager(PagingConfig(pageSize = 10)) {
            object : PagingSource<Int, GifsInfo>() {
                override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? = null

                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GifsInfo> {
                    loads++
                    return LoadResult.Page(items, prevKey = null, nextKey = null)
                }
            }
        }.flow
    }

    private class FeedPresenter(main: CoroutineContext, cached: PagingData<GifsInfo>?) :
        PagingDataPresenter<GifsInfo>(mainContext = main, cachedPagingData = cached) {

        val ids: List<String> get() = snapshot().items.map { it.id }

        override suspend fun presentPagingDataEvent(event: PagingDataEvent<GifsInfo>) = Unit

        suspend fun awaitIds(vararg expected: String) {
            withTimeout(AWAIT_MS) {
                while (ids != expected.toList()) delay(POLL_MS)
            }
        }
    }

    private companion object {
        const val AWAIT_MS = 5_000L
        const val POLL_MS = 10L
    }
}

/** То, что `collectAsLazyPagingItems` показывает в первом кадре, ещё не подписавшись на поток. */
private fun Flow<PagingData<GifsInfo>>.cachedForNewScreen(): PagingData<GifsInfo>? =
    (this as? SharedFlow<PagingData<GifsInfo>>)?.replayCache?.firstOrNull()
