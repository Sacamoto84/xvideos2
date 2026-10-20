package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Страница пейджера, которую можно держать в ScreenModel, — по той же причине,
 * что [ScrollPosition]: `PagerState` держит окно предзагрузки со ссылкой на
 * экран и после пересоздания Activity не отпускает старую.
 */
@Stable
class PagerPosition(page: Int = 0) {

    /** Текущая страница на момент ухода пейджера с экрана. */
    var page: Int = page
        private set

    /** Состояние пейджера, пока он на экране; без экрана — `null`. */
    var state: PagerState? by mutableStateOf(null)
        private set

    /** Показанный пейджер листается, без экрана страница ждёт следующего показа. */
    suspend fun scrollToPage(page: Int) {
        val shown = state
        if (shown != null) shown.scrollToPage(page) else this.page = page
    }

    /**
     * Состояние пейджера на время композиции, с этой страницы. [pageCount]
     * читается лениво и обновляется на каждой композиции — как у
     * `rememberPagerState`: число страниц может прийти позже.
     */
    @Composable
    fun rememberState(pageCount: () -> Int): PagerState {
        val created = remember(this) { CountedPagerState(page, pageCount) }
        created.pageCountState.value = pageCount
        DisposableEffect(this, created) {
            state = created
            onDispose {
                page = created.currentPage
                if (state === created) state = null
            }
        }
        return created
    }
}

private class CountedPagerState(page: Int, pageCount: () -> Int) : PagerState(page, 0f) {
    val pageCountState = mutableStateOf(pageCount)
    override val pageCount: Int
        get() = pageCountState.value.invoke()
}
