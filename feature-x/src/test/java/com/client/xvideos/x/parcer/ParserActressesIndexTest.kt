package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.ActressesIndexDropdownType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ParserActressesIndexTest {

    @Test
    fun `parseActressesIndexPage correctly parses catalog filters, items and pagination`() {
        val html = """
            <!doctype html>
            <html>
            <body>
                <div class="ordered-label-list">
                    <ul>
                        <strong class="btn btn-default main label btn-text">Топ 76 230 порноактрис</strong>
                        <li>
                            <span class="btn btn-default main label btn-profiles-date-links btn-geo-links">Мировые модели ▼</span>
                            <ul class="profiles-date-links geo-links hidden">
                                <li class="active"><a href="/porn-actresses-index/from/russia/ever">Мировые модели</a></li>
                                <li><a href="/porn-actresses-index/south_africa/from/russia/ever">Южноафриканское модели</a></li>
                            </ul>
                        </li>
                        <li>
                            <span class="btn btn-default main label btn-profiles-date-links btn-profile-links">Порноактрисы ▼</span>
                            <ul class="profiles-date-links profile-links hidden">
                                <li><a href="/pornstars-index">Все типы моделей</a></li>
                                <li class="active"><a href="/porn-actresses-index">Порноактрисы</a></li>
                                <li><a href="/amateurs-index">Любители</a></li>
                            </ul>
                        </li>
                        <li>
                            <span class="btn btn-default main label btn-profiles-date-links btn-time-links">Рейтинг ▼</span>
                            <ul class="profiles-date-links time-links hidden">
                                <li><a href="/porn-actresses-index/from/worldwide/ever">Подписчиков со всего мира (за всё время)</a></li>
                                <li class="active"><a href="/porn-actresses-index/from/russia/ever">Подписчиков Россиянки (за всё время)</a></li>
                            </ul>
                        </li>
                    </ul>
                </div>
                <h5 class="bg-title grey">Рейтинг на этой странице основан на просмотрах</h5>
                <div class="pagination">
                    <ul>
                        <li><a class="active" href="">1</a></li>
                        <li><a href="/porn-actresses-index/from/russia/ever/1">2</a></li>
                        <li><a href="/porn-actresses-index/from/russia/ever/952" class="last-page">953</a></li>
                        <li><a href="/porn-actresses-index/from/russia/ever/1" class="no-page next-page">Следующий</a></li>
                    </ul>
                </div>
                <div class="mozaique">
                    <div id="profile_sweetie-fox1" class="thumb-block thumb-block-profile">
                        <div class="thumb-inside">
                            <div class="thumb">
                                <a href="/pornstars/sweetie-fox1">
                                    <img src="https://thumb.example.com/xv_18_t.jpg" id="pic_sweetie-fox1" />
                                </a>
                            </div>
                            <span class="flag flag-ru" title="Россия"></span>
                        </div>
                        <div class="thumb-under">
                            <p class="profile-name">
                                <strong>#1</strong>&nbsp;<a href="/pornstars/sweetie-fox1">Sweetie Fox</a>
                            </p>
                            <p class="profile-counts">
                                <span class="with-sub">867 видео</span>
                            </p>
                        </div>
                    </div>
                    <div id="profile_joy-sky" class="thumb-block thumb-block-profile">
                        <div class="thumb-inside">
                            <div class="thumb">
                                <a href="/models/joy-sky">
                                    <img src="https://thumb.example.com/joy_sky.jpg" />
                                </a>
                            </div>
                            <span class="flag flag-br" title="Бразилия"></span>
                        </div>
                        <div class="thumb-under">
                            <p class="profile-name">
                                <strong>#2</strong>&nbsp;<a href="/models/joy-sky">Joy Ski</a>
                            </p>
                            <p class="profile-counts">
                                <span class="with-sub">279 видео</span>
                            </p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val catalog = parseActressesIndexPage(html)

        assertEquals("Топ 76 230 порноактрис", catalog.totalCountTitle)
        assertEquals("Рейтинг на этой странице основан на просмотрах", catalog.subtitle)

        // Geo Filter
        assertEquals(ActressesIndexDropdownType.GEO, catalog.geoFilter.type)
        assertEquals("Мировые модели", catalog.geoFilter.activeTitle)
        assertEquals(2, catalog.geoFilter.options.size)
        assertTrue(catalog.geoFilter.options[0].isActive)
        assertEquals("/porn-actresses-index/from/russia/ever", catalog.geoFilter.options[0].urlPath)

        // Profile Type Filter
        assertEquals(ActressesIndexDropdownType.PROFILE_TYPE, catalog.profileTypeFilter.type)
        assertEquals("Порноактрисы", catalog.profileTypeFilter.activeTitle)
        assertEquals(3, catalog.profileTypeFilter.options.size)
        assertTrue(catalog.profileTypeFilter.options[1].isActive)

        // Time Sort Filter
        assertEquals(ActressesIndexDropdownType.TIME_SORT, catalog.timeSortFilter.type)
        assertEquals("Рейтинг", catalog.timeSortFilter.activeTitle)
        assertEquals(2, catalog.timeSortFilter.options.size)
        assertTrue(catalog.timeSortFilter.options[1].isActive)

        // Items
        assertEquals(2, catalog.items.size)

        val item1 = catalog.items[0]
        assertEquals("sweetie-fox1", item1.slug)
        assertEquals("Sweetie Fox", item1.name)
        assertEquals("#1", item1.rankText)
        assertEquals("https://thumb.example.com/xv_18_t.jpg", item1.avatarUrl)
        assertEquals("Россия", item1.country)
        assertEquals("ru", item1.countryCode)
        assertEquals("867 видео", item1.videoCount)
        assertEquals("/pornstars/sweetie-fox1", item1.profileUrl)
        assertEquals("🇷🇺", item1.flagEmoji)

        val item2 = catalog.items[1]
        assertEquals("joy-sky", item2.slug)
        assertEquals("Joy Ski", item2.name)
        assertEquals("#2", item2.rankText)
        assertEquals("Бразилия", item2.country)
        assertEquals("br", item2.countryCode)
        assertEquals("279 видео", item2.videoCount)
        assertEquals("🇧🇷", item2.flagEmoji)

        // Pagination
        assertTrue(catalog.hasNextPage)
        assertEquals("/porn-actresses-index/from/russia/ever/1", catalog.nextPageUrl)
        assertEquals(953, catalog.totalPages)
        assertEquals(0, catalog.currentPage)
    }

    @Test
    fun `parseActressesIndexPage parses real actresses-index html file if present`() {
        val file = File("actresses-index.html")
        if (!file.exists()) return

        val html = file.readText()
        val catalog = parseActressesIndexPage(html)

        assertTrue(catalog.totalCountTitle.contains("76") || catalog.totalCountTitle.isNotBlank())
        assertEquals(80, catalog.items.size)

        val firstItem = catalog.items[0]
        assertEquals("sweetie-fox1", firstItem.slug)
        assertEquals("Sweetie Fox", firstItem.name)
        assertEquals("#1", firstItem.rankText)
        assertEquals("ru", firstItem.countryCode)
        assertTrue(firstItem.avatarUrl.isNotBlank())
        assertTrue(firstItem.videoCount.contains("видео") || firstItem.videoCount.isNotBlank())

        assertTrue(catalog.hasNextPage)
        assertTrue(catalog.nextPageUrl.contains("/porn-actresses-index/"))
    }

    @Test
    fun `parseActressesIndexPage handles blank html safely`() {
        val catalog = parseActressesIndexPage("")
        assertEquals("", catalog.totalCountTitle)
        assertEquals(0, catalog.items.size)
        assertFalse(catalog.hasNextPage)
    }
}
