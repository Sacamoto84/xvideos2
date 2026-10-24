package com.client.xvideos.x.feature.category

import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.screens.channel.model.buildProfileRanksUrl
import com.client.xvideos.x.screens.dashboards.buildDashboardUrl
import com.client.xvideos.x.screens.tags.buildTagPageUrl
import com.client.xvideos.x.search.buildSearchVideosUrl
import com.client.xvideos.x.urlStart
import org.junit.Assert.assertEquals
import org.junit.Test

/** Адреса сайта для выбранной категории — те же, что сайт даёт в своих ссылках. */
class XCategoryUrlsTest {

    @Test
    fun `первая страница ленты категории — её главная без номера`() {
        assertEquals("$urlStart/gay", buildDashboardUrl(0, XCategory.GAY))
        assertEquals("$urlStart/trans", buildDashboardUrl(0, XCategory.TRANS))
    }

    @Test
    fun `следующие страницы ленты категории — номер после её адреса`() {
        assertEquals("$urlStart/gay/1", buildDashboardUrl(1, XCategory.GAY))
        assertEquals("$urlStart/trans/42", buildDashboardUrl(42, XCategory.TRANS))
    }

    @Test
    fun `гетеро-лента — прежние адреса`() {
        assertEquals(urlStart, buildDashboardUrl(0, XCategory.STRAIGHT))
        assertEquals("$urlStart/new/3", buildDashboardUrl(3, XCategory.STRAIGHT))
    }

    @Test
    fun `поиск в категории — фильтр typef, как в пагинации выдачи сайта`() {
        assertEquals("$urlStart/?k=fox&typef=shemale", buildSearchVideosUrl("fox", 0, XCategory.TRANS))
        assertEquals("$urlStart/?k=fox&typef=gay&p=2", buildSearchVideosUrl("fox", 2, XCategory.GAY))
    }

    @Test
    fun `гетеро-поиск — без фильтра`() {
        assertEquals("$urlStart/?k=fox&p=1", buildSearchVideosUrl("fox", 1, XCategory.STRAIGHT))
    }

    @Test
    fun `тег в категории — с префиксом категории перед тегом`() {
        assertEquals("$urlStart/tags/t:shemale/big-tits/0", buildTagPageUrl("big-tits", 0, XCategory.TRANS))
        assertEquals("$urlStart/tags/t:gay/amateur/3", buildTagPageUrl("amateur", 3, XCategory.GAY))
    }

    @Test
    fun `гетеро-тег — прежний адрес`() {
        assertEquals("$urlStart/tags/blonde/1", buildTagPageUrl("blonde", 1, XCategory.STRAIGHT))
    }

    @Test
    fun `рейтинг канала — по выбранной категории`() {
        assertEquals("$urlStart/profiles/some_channel/ranks/straight", buildProfileRanksUrl("some_channel", XCategory.STRAIGHT))
        assertEquals("$urlStart/profiles/some_channel/ranks/gay", buildProfileRanksUrl("some_channel", XCategory.GAY))
        assertEquals("$urlStart/profiles/some_channel/ranks/shemale", buildProfileRanksUrl("some_channel", XCategory.TRANS))
    }
}
