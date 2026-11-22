package com.client.xvideos.r.common.pagin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NichePageNavigationTest {

    @Test
    fun `выбор страницы сохраняется при переключении режимов`() {
        val navigation = NichePageNavigation(infiniteScroll = true)
        navigation.updatePageCount(infiniteScroll = true, count = 45)
        assertTrue(navigation.state.value.infiniteScroll)

        navigation.setInfiniteScroll(false)
        assertEquals(45, navigation.state.value.pageCount)
        assertEquals(0, navigation.state.value.pageIndex)
        navigation.selectPage(9)
        assertEquals(9, navigation.state.value.pageIndex)

        navigation.setInfiniteScroll(true)
        navigation.selectPage(0)
        navigation.setInfiniteScroll(false)
        assertEquals(9, navigation.state.value.pageIndex)
    }

    @Test
    fun `выбранная страница остаётся в пределах актуального ответа`() {
        val navigation = NichePageNavigation(infiniteScroll = false)
        navigation.updatePageCount(infiniteScroll = false, count = 45)
        navigation.selectPage(80)
        assertEquals(44, navigation.state.value.pageIndex)

        navigation.updatePageCount(infiniteScroll = true, count = 8)
        assertEquals(44, navigation.state.value.pageIndex)
        navigation.updatePageCount(infiniteScroll = false, count = 32)
        assertEquals(31, navigation.state.value.pageIndex)

        navigation.resetForSort()
        assertEquals(0, navigation.state.value.pageIndex)
        assertEquals(1, navigation.state.value.pageCount)
    }
}
