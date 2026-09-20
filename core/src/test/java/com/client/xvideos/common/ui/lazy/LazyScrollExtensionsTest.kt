package com.client.xvideos.common.ui.lazy

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LazyScrollExtensionsTest {

    @Test
    fun `LazyListState isScrolled возвращает false когда список в самом начале`() {
        val state = LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
        assertFalse(state.isScrolled)
    }

    @Test
    fun `LazyListState isScrolled возвращает true при прокрутке по смещению внутри первого элемента`() {
        val state = LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 15)
        assertTrue(state.isScrolled)
    }

    @Test
    fun `LazyListState isScrolled возвращает true при прокрутке дальше первого элемента`() {
        val state = LazyListState(firstVisibleItemIndex = 2, firstVisibleItemScrollOffset = 0)
        assertTrue(state.isScrolled)
    }

    @Test
    fun `LazyGridState isScrolled возвращает false когда сетка в самом начале`() {
        val state = LazyGridState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
        assertFalse(state.isScrolled)
    }

    @Test
    fun `LazyGridState isScrolled возвращает true при частичной прокрутке по смещению`() {
        val state = LazyGridState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 50)
        assertTrue(state.isScrolled)
    }

    @Test
    fun `LazyGridState isScrolled возвращает true когда первый видимый элемент больше нуля`() {
        val state = LazyGridState(firstVisibleItemIndex = 4, firstVisibleItemScrollOffset = 0)
        assertTrue(state.isScrolled)
    }
}
