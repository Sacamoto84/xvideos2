package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import com.client.xvideos.l.model.PicsDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class LCollectionVisibleItemsTest {

    // С диска коллекция приходит от новых к старым.
    private val newImage = PicsDetails(id = "1", url_to_original = "new.jpg")
    private val gif = PicsDetails(id = "2", is_animated = true, url_to_original = "mid.gif")
    private val video = PicsDetails(id = "3", url_to_video = "clip.mp4")
    private val oldImage = PicsDetails(id = "4", url_to_original = "old.jpg")
    private val items = listOf(newImage, gif, video, oldImage)

    private fun ids(list: List<PicsDetails>) = list.map { it.id }

    @Test
    fun `все, сначала новые - как с диска`() {
        assertEquals(
            ids(items),
            ids(lCollectionVisibleItems(items, LCollectionTypeFilter.ALL, LCollectionItemsOrder.NEWEST))
        )
    }

    @Test
    fun `картинки - без gif и видео`() {
        assertEquals(
            listOf("1", "4"),
            ids(lCollectionVisibleItems(items, LCollectionTypeFilter.IMAGES, LCollectionItemsOrder.NEWEST))
        )
    }

    @Test
    fun `gif - анимированные и с видео`() {
        assertEquals(
            listOf("2", "3"),
            ids(lCollectionVisibleItems(items, LCollectionTypeFilter.GIFS, LCollectionItemsOrder.NEWEST))
        )
    }

    @Test
    fun `сначала старые - обратный порядок и вместе с отбором по типу`() {
        assertEquals(
            listOf("4", "3", "2", "1"),
            ids(lCollectionVisibleItems(items, LCollectionTypeFilter.ALL, LCollectionItemsOrder.OLDEST))
        )
        assertEquals(
            listOf("4", "1"),
            ids(lCollectionVisibleItems(items, LCollectionTypeFilter.IMAGES, LCollectionItemsOrder.OLDEST))
        )
    }

    @Test
    fun `пустая коллекция - пустой список`() {
        assertEquals(
            emptyList<String>(),
            ids(lCollectionVisibleItems(emptyList(), LCollectionTypeFilter.GIFS, LCollectionItemsOrder.OLDEST))
        )
    }

    @Test
    fun `подписи фильтров - по порядку вкладок`() {
        assertEquals(listOf("Все", "Картинки", "Gif"), LCollectionTypeFilter.entries.map { it.title })
    }

    @Test
    fun `порядки в меню - сначала новые, потом старые`() {
        assertEquals(listOf("Сначала новые", "Сначала старые"), LCollectionItemsOrder.entries.map { it.title })
    }
}
