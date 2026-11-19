package com.client.xvideos.n.ui.screens.explorer.tab.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.common.ui.scroll.PagerPosition
import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.repository.CloudflareBlockedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Кэш страниц и позиций одного списка. Все изменения публикуются в области экрана. */
class NGalleryPages(
    scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fetch: suspend (pageNumber: Int) -> Result<NGalleryPageResult>
) {
    private val loads = SupervisorJob(scope.coroutineContext[Job])
    private val loadScope = CoroutineScope(scope.coroutineContext + loads)
    private val states = mutableStateMapOf<Int, NGalleryListUiState>()
    private val grids = mutableMapOf<Int, LazyGridPosition>()

    var position by mutableStateOf(PagerPosition())
        private set
    var totalPages by mutableIntStateOf(1)
        private set

    val currentIndex: Int get() = position.state?.currentPage ?: position.page
    val loading: Boolean get() = states.containsValue(NGalleryListUiState.Loading)
    internal val visibleChallenge: CloudflareBlockedException?
        get() = (states[currentIndex] as? NGalleryListUiState.CloudflareRequired)?.challenge

    operator fun get(index: Int): NGalleryListUiState? = states[index]

    fun grid(index: Int): LazyGridPosition = grids.getOrPut(index) { LazyGridPosition() }

    fun load(index: Int) {
        if (index !in 0 until totalPages || states.containsKey(index)) return
        states[index] = NGalleryListUiState.Loading
        loadScope.launch {
            try {
                val result = withContext(dispatcher) { fetch(index + 1).getOrThrow() }
                ensureActive()
                totalPages = result.totalPages.coerceAtLeast(1)
                states[index] = NGalleryListUiState.Success(result.items)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (blocked: CloudflareBlockedException) {
                ensureActive()
                states[index] = NGalleryListUiState.CloudflareRequired(blocked)
            } catch (error: Exception) {
                ensureActive()
                states[index] = NGalleryListUiState.Error(error.localizedMessage ?: "Ошибка загрузки страницы")
            }
        }
    }

    fun retry(index: Int) {
        if (states[index] == NGalleryListUiState.Loading) return
        states.remove(index)
        load(index)
    }

    fun reset() {
        loads.cancelChildren()
        states.clear()
        grids.clear()
        totalPages = 1
        position = PagerPosition()
    }

    /** После изменения серверного списка элементы могут сместиться на соседние страницы. */
    fun refresh() {
        val index = currentIndex
        loads.cancelChildren()
        states.clear()
        position = PagerPosition(index.coerceIn(0, totalPages - 1))
    }
}
