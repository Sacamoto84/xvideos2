package com.client.xvideos.common.ui.scroll

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Без экрана позиция — просто числа: прокрутка к элементу запоминается до
 * следующего показа, и состояния списка позиция не держит.
 */
class ScrollPositionTest {

    @Test
    fun `новая позиция начинается с переданного элемента и без состояния`() {
        val position = LazyGridPosition(index = 7, offset = 40)

        assertEquals(7, position.index)
        assertEquals(40, position.offset)
        assertNull(position.state)
    }

    @Test
    fun `прокрутка без экрана становится позицией следующего показа`() = runBlocking {
        val list = LazyListPosition(index = 12, offset = 5)
        val staggered = LazyStaggeredGridPosition(index = 3)

        list.scrollToItem(0)
        staggered.scrollToItem(9, 20)

        assertEquals(0, list.index)
        assertEquals(0, list.offset)
        assertEquals(9, staggered.index)
        assertEquals(20, staggered.offset)
        assertNull(list.state)
    }

    @Test
    fun `страница пейджера без экрана запоминается до показа`() = runBlocking {
        val position = PagerPosition(page = 4)

        position.scrollToPage(0)

        assertEquals(0, position.page)
        assertNull(position.state)
    }
}
