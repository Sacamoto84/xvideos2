package com.client.xvideos.l.model

import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AlbumFilterPresetManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AlbumFilterPresetManagerTest {

    @Before
    fun setUp() {
        AlbumFilterPresetManager.resetForTesting()
    }

    @Test
    fun `generateDefaultName с пустыми параметрами формирует Preset N`() {
        val filter = AlbumListFilter(album_type = AlbumType.All)
        val name = AlbumFilterPresetManager.generateDefaultName(filter)
        assertEquals("Preset 1", name)
    }

    @Test
    fun `generateDefaultName с поисковым запросом и типом контента объединяет параметры через разделитель`() {
        val filter = AlbumListFilter(
            searchQuery = "gothic lolita",
            album_type = AlbumType.Pictures,
            content_id = ContentId.Hentai,
            selection = "animated"
        )
        val name = AlbumFilterPresetManager.generateDefaultName(filter)
        assertEquals("gothic lolita • Pictures • Hentai • Animated", name)
    }

    @Test
    fun `generateDefaultName с жанрами и тегами указывает количество`() {
        val filter = AlbumListFilter(
            album_type = AlbumType.All,
            genresPlus = listOf(FilterGenre(id = "1", title = "Romance"), FilterGenre(id = "2", title = "Fantasy")),
            tagPlus = listOf("elf", "magic", "dress")
        )
        val name = AlbumFilterPresetManager.generateDefaultName(filter)
        assertEquals("+2 genres • +3 tags", name)
    }

    @Test
    fun `formatFilterSummary выводит ключевые параметры фильтра`() {
        val filter = AlbumListFilter(
            album_type = AlbumType.Manga,
            content_id = ContentId.Hentai,
            picture_count_rank = PictureCountRank.C50_100,
            genresPlus = listOf(FilterGenre(id = "1", title = "Romance")),
            genresMinus = listOf(FilterGenre(id = "2", title = "Horror")),
            tagPlus = listOf("cute"),
            tagMinus = listOf("ugly"),
            selection = "animated"
        )
        val summary = AlbumFilterPresetManager.formatFilterSummary(filter)
        assertTrue(summary.contains("Type: Manga"))
        assertTrue(summary.contains("Content: Hentai"))
        assertTrue(summary.contains("Size: 50..100"))
        assertTrue(summary.contains("+Romance"))
        assertTrue(summary.contains("NOT Horror"))
        assertTrue(summary.contains("+tags: cute"))
        assertTrue(summary.contains("-tags: ugly"))
        assertTrue(summary.contains("Animated"))
    }

    @Test
    fun `resetForTesting корректно обновляет состояние пресетов`() {
        val preset = SavedAlbumFilter(
            id = "test-1",
            name = "Test Preset",
            filter = AlbumListFilter()
        )
        AlbumFilterPresetManager.resetForTesting(listOf(preset))
        assertEquals(1, AlbumFilterPresetManager.presets.value.size)
        assertEquals("Test Preset", AlbumFilterPresetManager.presets.value.first().name)
    }
}
