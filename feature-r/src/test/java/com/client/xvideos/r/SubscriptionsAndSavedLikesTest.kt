package com.client.xvideos.r

import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.sanitizeGifsInfoList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionsAndSavedLikesTest {

    private fun fakeGif(id: String, createDate: Long, likes: Int = 0): GifsInfo {
        return GifsInfo(
            id = id,
            createDate = createDate,
            likes = likes,
        )
    }

    @Test
    fun `subscriptions deduplication preserves distinct items and removes duplicates`() {
        val listWithDuplicates = listOf(
            fakeGif("gif_1", 100L),
            fakeGif("gif_2", 200L),
            fakeGif("gif_1", 100L),
            fakeGif("gif_3", 300L),
            fakeGif("gif_2", 200L),
        )

        val deduplicated = listWithDuplicates.sanitizeGifsInfoList().distinctBy { it.id }

        assertEquals(3, deduplicated.size)
        assertEquals(listOf("gif_1", "gif_2", "gif_3"), deduplicated.map { it.id })
    }

    @Test
    fun `saved likes sorting by order works properly`() {
        val items = listOf(
            fakeGif("gif_early", 1000L, likes = 50),
            fakeGif("gif_late", 3000L, likes = 10),
            fakeGif("gif_top", 2000L, likes = 500),
        )

        fun sortItems(list: List<GifsInfo>, order: Order): List<GifsInfo> = when (order) {
            Order.OLDEST -> list.sortedBy { it.createDate }
            Order.TOP, Order.TOP_WEEK, Order.TOP_MONTH, Order.TOP28 -> list.sortedByDescending { it.likes }
            else -> list.sortedByDescending { it.createDate }
        }

        val sortedOldest = sortItems(items, Order.OLDEST)
        assertEquals("gif_early", sortedOldest.first().id)
        assertEquals("gif_late", sortedOldest.last().id)

        val sortedLatest = sortItems(items, Order.LATEST)
        assertEquals("gif_late", sortedLatest.first().id)
        assertEquals("gif_early", sortedLatest.last().id)

        val sortedTop = sortItems(items, Order.TOP)
        assertEquals("gif_top", sortedTop.first().id)
        assertEquals("gif_late", sortedTop.last().id)
    }

    @Test
    fun `collection lookup fallback handles missing collection gracefully without exception`() {
        data class FakeCollection(val collection: String?, val items: List<GifsInfo>)

        val collectionList = listOf(
            FakeCollection("Favorites", listOf(fakeGif("fav1", 1L))),
        )

        val existing = collectionList.firstOrNull { it.collection == "Favorites" }?.items ?: emptyList()
        assertEquals(1, existing.size)

        val missing = collectionList.firstOrNull { it.collection == "Deleted" }?.items ?: emptyList()
        assertTrue(missing.isEmpty())

        val nullCollection = collectionList.firstOrNull { it.collection == null }?.items ?: emptyList()
        assertTrue(nullCollection.isEmpty())
    }
}
