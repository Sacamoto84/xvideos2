package com.client.xvideos.r.common.pagin

import com.client.xvideos.r.model.Order
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPageNavigationTest {
    private val latest = FeedPageQuery(Order.LATEST)

    @Test
    fun `пейджер учитывает ограничение API даже когда сервер сообщает больше страниц`() {
        val navigation = FeedPageNavigation(latest, infiniteScroll = false)
        navigation.updatePageCount(latest, infiniteScroll = false, count = 9684)
        navigation.selectPage(latest, 100)

        assertEquals(100, navigation.state.value.pageCount)
        assertEquals(99, navigation.state.value.pageIndex)
    }

    @Test
    fun `выбор страницы сохраняется при переключении режимов`() {
        val navigation = FeedPageNavigation(latest, infiniteScroll = true)
        navigation.updatePageCount(latest, infiniteScroll = true, count = 45)
        assertTrue(navigation.state.value.infiniteScroll)

        navigation.setInfiniteScroll(false)
        assertEquals(45, navigation.state.value.pageCount)
        assertEquals(0, navigation.state.value.pageIndex)
        navigation.selectPage(latest, 9)
        assertEquals(9, navigation.state.value.pageIndex)

        navigation.setInfiniteScroll(true)
        navigation.selectPage(latest, 0)
        navigation.setInfiniteScroll(false)
        assertEquals(9, navigation.state.value.pageIndex)
    }

    @Test
    fun `выбранная страница остаётся в пределах актуального ответа`() {
        val navigation = FeedPageNavigation(latest, infiniteScroll = false)
        navigation.updatePageCount(latest, infiniteScroll = false, count = 45)
        navigation.selectPage(latest, 80)
        assertEquals(44, navigation.state.value.pageIndex)

        navigation.updatePageCount(latest, infiniteScroll = true, count = 8)
        assertEquals(44, navigation.state.value.pageIndex)
        navigation.updatePageCount(latest, infiniteScroll = false, count = 32)
        assertEquals(31, navigation.state.value.pageIndex)

        navigation.updateQuery(FeedPageQuery(Order.TOP))
        assertEquals(0, navigation.state.value.pageIndex)
        assertEquals(1, navigation.state.value.pageCount)
    }

    @Test
    fun `смена тегов сбрасывает страницу и игнорирует запоздавший ответ`() {
        val navigation = FeedPageNavigation(latest, infiniteScroll = false)
        navigation.updatePageCount(latest, infiniteScroll = false, count = 12)
        navigation.selectPage(latest, 5)

        val filtered = latest.copy(tags = setOf("verified"))
        navigation.updateQuery(filtered)
        navigation.updatePageCount(latest, infiniteScroll = false, count = 12)
        navigation.updatePageCount(filtered, infiniteScroll = false, count = 2)
        navigation.selectPage(latest, 5)

        assertEquals(filtered, navigation.state.value.query)
        assertEquals(0, navigation.state.value.pageIndex)
        assertEquals(2, navigation.state.value.pageCount)
    }
}
