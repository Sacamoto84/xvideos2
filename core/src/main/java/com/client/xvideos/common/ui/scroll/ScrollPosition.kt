package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Позиция ленивого списка или сетки, которую можно держать в ScreenModel.
 *
 * Сами `LazyListState`, `LazyGridState` и `LazyStaggeredGridState` в ScreenModel
 * держать нельзя: они запоминают узел своей раскладки и последний замер и не
 * отпускают их после ухода из композиции, а после пересоздания Activity держат
 * старую (проход 199, T1). Здесь — номер первого видимого элемента и смещение,
 * а объект состояния создаёт композиция ([rememberState]) и отпускает при уходе
 * из неё, записав позицию.
 */
@Stable
abstract class ScrollPosition<S : Any> internal constructor(index: Int, offset: Int) {

    /** Номер первого видимого элемента на момент ухода списка с экрана. */
    var index: Int = index
        private set

    /** Смещение первого видимого элемента в пикселях. */
    var offset: Int = offset
        private set

    /** Состояние списка, пока он на экране; без экрана — `null`. */
    var state: S? by mutableStateOf(null)
        private set

    internal abstract fun create(index: Int, offset: Int): S

    internal abstract fun indexOf(state: S): Int

    internal abstract fun offsetOf(state: S): Int

    internal abstract suspend fun scroll(state: S, index: Int, offset: Int)

    /** Показанный список прокручивается, без экрана позиция ждёт следующего показа. */
    suspend fun scrollToItem(index: Int, offset: Int = 0) {
        val shown = state
        if (shown != null) {
            scroll(shown, index, offset)
        } else {
            this.index = index
            this.offset = offset
        }
    }

    /** Состояние списка на время композиции, с этой позиции. */
    @Composable
    fun rememberState(): S {
        val created = remember(this) { create(index, offset) }
        DisposableEffect(this, created) {
            state = created
            onDispose {
                index = indexOf(created)
                offset = offsetOf(created)
                if (state === created) state = null
            }
        }
        return created
    }
}

/** [ScrollPosition] ленивого списка. */
@OptIn(ExperimentalFoundationApi::class)
class LazyListPosition(
    index: Int = 0,
    offset: Int = 0,
) : ScrollPosition<LazyListState>(index, offset) {
    override fun create(index: Int, offset: Int) = LazyListState(index, offset)
    override fun indexOf(state: LazyListState) = state.firstVisibleItemIndex
    override fun offsetOf(state: LazyListState) = state.firstVisibleItemScrollOffset
    override suspend fun scroll(state: LazyListState, index: Int, offset: Int) = state.scrollToItem(index, offset)
}

/**
 * [ScrollPosition] ленивой сетки. Окно упреждающей загрузки — отдельным
 * конструктором: его тип экспериментальный, и в общей сигнатуре он требовал бы
 * opt-in у каждого вызова.
 */
@OptIn(ExperimentalFoundationApi::class)
class LazyGridPosition private constructor(
    index: Int,
    offset: Int,
    private val cacheWindow: LazyLayoutCacheWindow?,
) : ScrollPosition<LazyGridState>(index, offset) {

    constructor(index: Int = 0, offset: Int = 0) : this(index, offset, null)

    /** С окном упреждающей загрузки [cacheWindow]. */
    @ExperimentalFoundationApi
    constructor(cacheWindow: LazyLayoutCacheWindow, index: Int = 0, offset: Int = 0) : this(index, offset, cacheWindow)

    override fun create(index: Int, offset: Int) =
        if (cacheWindow != null) LazyGridState(cacheWindow, index, offset) else LazyGridState(index, offset)
    override fun indexOf(state: LazyGridState) = state.firstVisibleItemIndex
    override fun offsetOf(state: LazyGridState) = state.firstVisibleItemScrollOffset
    override suspend fun scroll(state: LazyGridState, index: Int, offset: Int) = state.scrollToItem(index, offset)
}

/** [ScrollPosition] шахматной сетки. */
class LazyStaggeredGridPosition(
    index: Int = 0,
    offset: Int = 0,
) : ScrollPosition<LazyStaggeredGridState>(index, offset) {
    override fun create(index: Int, offset: Int) = LazyStaggeredGridState(index, offset)
    override fun indexOf(state: LazyStaggeredGridState) = state.firstVisibleItemIndex
    override fun offsetOf(state: LazyStaggeredGridState) = state.firstVisibleItemScrollOffset
    override suspend fun scroll(state: LazyStaggeredGridState, index: Int, offset: Int) =
        state.scrollToItem(index, offset)
}
