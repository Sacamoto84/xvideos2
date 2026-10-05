package com.client.xvideos.l.ui.screens

import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.ui.screens.albumLandingTag.createAlbumTagFilter
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.createAlbumSearchFilter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * «Показать все» у раздела открывает тот же набор альбомов, что показан в
 * разделе. Набор задаёт ссылка раздела; заголовок — текст для человека, сайт
 * может его переименовать, и сравнение с ним уводило бы в запасную ветку.
 */
class LSectionFilterTest {

    private fun section(title: String, url: String = "") = Landing_page_albumSection(title = title, url = url)

    @Test
    fun `поиск - тип альбома берётся из ссылки раздела, а не из заголовка`() {
        val renamed = section(title = "Comics", url = "/albums/list/?album_type=manga&search_query=abc")

        val filter = createAlbumSearchFilter(renamed, "abc")

        assertEquals(AlbumType.Manga, filter.album_type)
        assertEquals("abc", filter.searchQuery)
        assertEquals("search_score", filter.display)
    }

    @Test
    fun `поиск - без параметров в ссылке остаётся сравнение по заголовку`() {
        assertEquals(AlbumType.Manga, createAlbumSearchFilter(section("Manga"), "abc").album_type)
        assertEquals(AlbumType.Pictures, createAlbumSearchFilter(section("Picture Sets", url = "/albums/"), "abc").album_type)
    }

    @Test
    fun `тег - тип альбома и контента берутся из ссылки раздела`() {
        val renamed = section(title = "Photo Sets", url = "/albums/list/?album_type=pictures&content_id=6&tagged=%2Bx")

        val filter = createAlbumTagFilter(renamed, tag = "x")

        assertEquals(AlbumType.Pictures, filter.album_type)
        assertEquals(ContentId.RealPeople, filter.content_id)
        assertEquals(listOf("x"), filter.tagPlus)
        assertEquals("date_trending", filter.display)
    }

    @Test
    fun `тег - неизвестное значение в ссылке не подменяет фильтр мусором`() {
        val odd = section(title = "Something", url = "/albums/list/?album_type=zzz&content_id=abc")

        val filter = createAlbumTagFilter(odd, tag = "x")

        assertEquals(AlbumType.Pictures, filter.album_type)
        assertEquals(ContentId.All, filter.content_id)
    }
}
