package com.client.xvideos.r.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Замена сортировки, выпавшей из набора.
 *
 * Набор в ленте гифок разный без поиска и с поиском, и выбранное значение может
 * в новом наборе отсутствовать. Раньше в таком случае жёстко ставился
 * `Order.LATEST`: выбрал «All time», начал искать — искалось по «Latest», без
 * единого сообщения.
 *
 * Списки здесь — те же, что в `R_ScreenGifsTab`. Если они там поменяются,
 * должен поменяться и тест: это и есть смысл дублирования.
 */
class OrderNearestInTest {

    private val feedOrders = listOf(
        Order.TOP_WEEK, Order.TOP_MONTH, Order.TOP_ALLTIME, Order.TRENDING, Order.LATEST
    )

    private val searchOrders = listOf(
        Order.RELEVANT, Order.TOP, Order.TOP_WEEK, Order.TOP_MONTH, Order.TRENDING, Order.LATEST
    )

    @Test
    fun `сортировка из набора остаётся как есть`() {
        for (order in feedOrders) assertEquals(order, order.nearestIn(feedOrders))
        for (order in searchOrders) assertEquals(order, order.nearestIn(searchOrders))
    }

    @Test
    fun `All time при переходе к поиску становится Top, а не Latest`() {
        assertEquals(Order.TOP, Order.TOP_ALLTIME.nearestIn(searchOrders))
    }

    @Test
    fun `Relevant при возврате к ленте становится Trending, а не Latest`() {
        assertEquals(Order.TRENDING, Order.RELEVANT.nearestIn(feedOrders))
    }

    @Test
    fun `Top при возврате к ленте становится Week`() {
        assertEquals(Order.TOP_WEEK, Order.TOP.nearestIn(feedOrders))
    }

    /**
     * Своей ленты у `TOP_ALLTIME` нет: `ItemTopPagingSource` уводит его в
     * `getTopThisWeek`. Пока это так, замена не имеет права никого туда
     * приводить — выбрать вручную пользователь может, привести его насильно мы
     * не должны.
     */
    @Test
    fun `замена никогда не приводит в TOP_ALLTIME`() {
        for (order in Order.entries) {
            assertTrue(
                "$order -> ${order.nearestIn(feedOrders)}",
                order.nearestIn(feedOrders) != Order.TOP_ALLTIME || order == Order.TOP_ALLTIME
            )
        }
    }

    /** Результат обязан лежать в наборе — иначе `LaunchedEffect` зациклится. */
    @Test
    fun `результат всегда из набора`() {
        for (order in Order.entries) {
            assertTrue("$order", order.nearestIn(feedOrders) in feedOrders)
            assertTrue("$order", order.nearestIn(searchOrders) in searchOrders)
        }
    }

    /** Пустой набор менять не на что — возвращаем исходное, без исключения. */
    @Test
    fun `пустой набор оставляет сортировку прежней`() {
        assertEquals(Order.TOP_ALLTIME, Order.TOP_ALLTIME.nearestIn(emptyList()))
    }
}
