package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumListFilterGenresTest {

    @Test
    fun `без счётчиков доступны жанры каталога кроме уже выбранных`() {
        val action = FilterGenre(id = "1", title = "Action")
        val fantasy = FilterGenre(id = "2", title = "Fantasy")

        val genres = selectableAlbumGenres(listOf(action, fantasy), emptyList(), setOf("1"))

        assertEquals(listOf(fantasy), genres)
    }

    @Test
    fun `без каталога ID жанра берётся из ссылки агрегации текущего фильтра`() {
        val counts = listOf(
            AlbumListFilterGenreCountResponse(
                count = 6,
                term = "Fantasy",
                urlToMustInclude = "/albums/list/?genre_ids=%2B1%2B2-4",
            ),
            AlbumListFilterGenreCountResponse(count = 8, term = "Drama"),
        )

        val genres = selectableAlbumGenres(emptyList(), counts, setOf("1", "4"))

        assertEquals(listOf(FilterGenre(id = "2", title = "Fantasy")), genres)
    }
}
