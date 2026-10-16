package com.client.xvideos.x.screens.subscriptions

import com.client.xvideos.x.model.XSubscriptionItem
import org.junit.Assert.assertEquals
import org.junit.Test

class XSortedSubscriptionsTest {

    // Порядок «с диска» — по времени файла, от новых к старым.
    private val beta = XSubscriptionItem(slug = "beta", name = "beta", videoCount = 10, dateAdded = 3_000)
    private val alpha = XSubscriptionItem(slug = "alpha", name = "Alpha", videoCount = 250, dateAdded = 1_000)
    private val gamma = XSubscriptionItem(slug = "gamma", name = "", videoCount = 10, dateAdded = 2_000)
    private val items = listOf(beta, gamma, alpha)

    private fun slugs(order: XSubscriptionsOrder) = xSortedSubscriptions(items, order).map { it.slug }

    @Test
    fun `сначала новые и сначала старые - по дате добавления`() {
        assertEquals(listOf("beta", "gamma", "alpha"), slugs(XSubscriptionsOrder.NEWEST))
        assertEquals(listOf("alpha", "gamma", "beta"), slugs(XSubscriptionsOrder.OLDEST))
    }

    @Test
    fun `по имени - без учёта регистра, пустое имя заменяет адрес канала`() {
        assertEquals(listOf("alpha", "beta", "gamma"), slugs(XSubscriptionsOrder.NAME))
    }

    @Test
    fun `по числу видео - от большего, при равенстве по имени`() {
        assertEquals(listOf("alpha", "beta", "gamma"), slugs(XSubscriptionsOrder.VIDEOS))
    }

    @Test
    fun `одинаковая дата - прежний порядок`() {
        val a = XSubscriptionItem(slug = "a", dateAdded = 5)
        val b = XSubscriptionItem(slug = "b", dateAdded = 5)
        assertEquals(listOf("b", "a"), xSortedSubscriptions(listOf(b, a), XSubscriptionsOrder.NEWEST).map { it.slug })
    }

    @Test
    fun `сохранённый номер порядка - вне списка даёт порядок по умолчанию`() {
        assertEquals(XSubscriptionsOrder.NAME, xSubscriptionsOrderOf(XSubscriptionsOrder.NAME.ordinal))
        assertEquals(XSubscriptionsOrder.NEWEST, xSubscriptionsOrderOf(-1))
        assertEquals(XSubscriptionsOrder.NEWEST, xSubscriptionsOrderOf(99))
    }

    @Test
    fun `пункты меню - по порядку`() {
        assertEquals(
            listOf("Сначала новые", "Сначала старые", "По имени", "По числу видео"),
            XSubscriptionsOrder.entries.map { it.title }
        )
    }
}
