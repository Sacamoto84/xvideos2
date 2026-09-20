package com.client.xvideos.common.ui.lazy

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState

/**
 * Возвращает `true`, если список смещён относительно своего начала
 * (первый элемент прокручен дальше нулевого индекса или имеет ненулевой offset).
 */
val LazyListState.isScrolled: Boolean
    get() = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0

/**
 * Возвращает `true`, если сетка смещена относительно своего начала
 * (первый элемент прокручен дальше нулевого индекса или имеет ненулевой offset).
 */
val LazyGridState.isScrolled: Boolean
    get() = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0

/**
 * Возвращает `true`, если ступенчатая сетка смещена относительно своего начала
 * (первый элемент прокручен дальше нулевого индекса или имеет ненулевой offset).
 */
val LazyStaggeredGridState.isScrolled: Boolean
    get() = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0

