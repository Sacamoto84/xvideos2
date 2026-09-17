package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits

import com.client.xvideos.l.model.enum.AlbumType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class L_ScreenAlbumTopHitsTest {

    @Test
    fun `URL без query возвращает дефолтный фильтр`() {
        val filter = albumListFilterFromTopHitsUrl("https://example.com/albums/list/")
        assertEquals(AlbumType.Pictures, filter.album_type)
        assertEquals("date_newest", filter.display)
        assertTrue(filter.tagPlus.isEmpty())
    }

    @Test
    fun `парсинг параметров из URL верхних хитов`() {
        val url = "/albums/list/?album_type=manga&audience_ids=%2B1%2B2&display=rating_all_time&tagged=%2Bcollared%2Bleather&page=1"
        val filter = albumListFilterFromTopHitsUrl(url)

        assertEquals(AlbumType.Manga, filter.album_type)
        assertEquals("rating_all_time", filter.display)
        assertEquals("+1+2", filter.audienceIds)
        assertEquals(listOf("collared", "leather"), filter.tagPlus)
    }

    @Test
    fun `некорректный album_type откатывается на дефолт`() {
        val url = "/albums/list/?album_type=unknown_type"
        val filter = albumListFilterFromTopHitsUrl(url)

        assertEquals(AlbumType.Pictures, filter.album_type)
    }

    @Test
    fun `парсинг тегов фильтрует пустые токены`() {
        val url = "/albums/list/?tagged=%2B%2Btag1%2B%20%2Btag2%2B"
        val filter = albumListFilterFromTopHitsUrl(url)

        assertEquals(listOf("tag1", "tag2"), filter.tagPlus)
    }

    @Test
    fun `битые пары ключ-значение без разделителя игнорируются`() {
        val url = "/albums/list/?broken&display=date_trending&=emptyKey"
        val filter = albumListFilterFromTopHitsUrl(url)

        assertEquals("date_trending", filter.display)
    }
}
