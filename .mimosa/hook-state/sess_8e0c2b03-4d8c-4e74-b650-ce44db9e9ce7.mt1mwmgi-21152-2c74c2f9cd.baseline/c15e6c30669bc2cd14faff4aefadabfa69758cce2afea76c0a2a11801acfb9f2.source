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
        Order.TOP_WEEK, Order.TOP_MONTH, Order.TOP, Order.TRENDING, Order.LATEST
    )

    private val searchOrders = listOf(
        Order.RELEVANT, Order.TOP, Order.TOP_WEEK, Order.TOP_MONTH, Order.TRENDING, Order.LATEST
    )

    @Test
    fun `сортировка из набора остаётся как есть`() {
        for (order in feedOrders) assertEquals(order, order.nearestIn(feedOrders))
        for (order in searchOrders) assertEquals(order, order.nearestIn(searchOrders))
    }

    /**
     * Relevance у лент аналога не имеет, поэтому её и заменяют. Top — ближайшее
     * по смыслу: это «топ за всё время», а не за окно.
     */
    @Test
    fun `Relevant при возврате к ленте становится Top, а не Latest`() {
        assertEquals(Order.TOP, Order.RELEVANT.nearestIn(feedOrders))
    }

    /** Единственное расхождение наборов — Relevant; всё прочее общее. */
    @Test
    fun `наборы ленты и поиска различаются одним элементом`() {
        assertEquals(setOf(Order.RELEVANT), searchOrders.toSet() - feedOrders.toSet())
        assertEquals(emptySet<Order>(), feedOrders.toSet() - searchOrders.toSet())
    }

    /**
     * Замена обязана попадать только в значения, которые сервер принимает.
     * `alltime` он отвергает с 400 BadOrder — потому `TOP_ALLTIME` и сведён к
     * [Order.TOP]. Сторож на случай, если в набор снова добавят несуществующее.
     */
    @Test
    fun `замена попадает только в принимаемые сервером значения`() {
        val acceptedByFeed = setOf("top", "top7", "top28", "latest", "score", "trending")
        for (order in Order.entries) {
            val nearest = order.nearestIn(feedOrders)
            assertTrue(
                "$order -> $nearest (${nearest.value})",
                nearest.value in acceptedByFeed
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
        assertEquals(Order.RELEVANT, Order.RELEVANT.nearestIn(emptyList()))
    }
}
