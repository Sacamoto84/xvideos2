package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveAgeRange
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLivePrivatePrice
import com.client.xvideos.xlive.model.XLiveRegion
import com.client.xvideos.xlive.model.XLiveSearchFilters
import com.client.xvideos.xlive.model.XLiveTagSort
import org.junit.Assert.assertEquals
import org.junit.Test

class XLiveRoomQueryTest {

    @Test
    fun `все и популярные — только страница`() {
        assertEquals("limit=90&offset=0", roomListQuery(XLiveGender.ALL, XLiveCollection.POPULAR, offset = 0))
    }

    @Test
    fun `пол и подборка добавляют свои параметры`() {
        assertEquals(
            "limit=90&offset=180&genders=f&new_cams=true",
            roomListQuery(XLiveGender.FEMALE, XLiveCollection.NEW, offset = 180)
        )
        assertEquals(
            "limit=90&offset=0&genders=t&sort=rand",
            roomListQuery(XLiveGender.TRANS, XLiveCollection.RANDOM, offset = 0)
        )
    }

    @Test
    fun `тег добавляется последним и кодируется`() {
        assertEquals(
            "limit=90&offset=90&genders=f&hashtags=asian",
            roomListQuery(XLiveGender.FEMALE, XLiveCollection.POPULAR, offset = 90, hashtag = " asian ")
        )
        assertEquals(
            "limit=90&offset=0&hashtags=%D1%82%D0%B5%D0%B3",
            roomListQuery(XLiveGender.ALL, XLiveCollection.POPULAR, offset = 0, hashtag = "тег")
        )
    }

    @Test
    fun `таблица тегов — порядок, страница, пол, лимит`() {
        assertEquals("sort=-vc&page=1&limit=100", tagTableQuery(XLiveGender.ALL, XLiveTagSort.VIEWERS, page = 1))
        assertEquals("sort=ht&page=3&g=t&limit=100", tagTableQuery(XLiveGender.TRANS, XLiveTagSort.NAME, page = 3))
    }

    @Test
    fun `слово поиска кодируется, пустое не передаётся`() {
        assertEquals(
            "limit=90&offset=0&genders=f&keywords=big%20goal",
            roomListQuery(XLiveGender.FEMALE, XLiveCollection.POPULAR, offset = 0, keywords = " big goal ")
        )
        assertEquals("limit=90&offset=0", roomListQuery(XLiveGender.ALL, XLiveCollection.POPULAR, offset = 0, keywords = "  "))
    }

    @Test
    fun `фильтры — возраст без верхней границы, регион и список цен через запятую`() {
        val filters = XLiveSearchFilters(
            age = XLiveAgeRange.AGE_18_21,
            region = XLiveRegion.EUROPE_RUSSIA,
            price = XLivePrivatePrice.PRICE_12_18,
        )
        assertEquals(
            "limit=90&offset=90&keywords=goal&from_age=18&to_age=22&regions=ER&private_prices=12,18",
            roomListQuery(XLiveGender.ALL, XLiveCollection.POPULAR, offset = 90, keywords = "goal", filters = filters)
        )
    }

    @Test
    fun `без фильтров — ни одного параметра фильтров`() {
        assertEquals(emptyList<Pair<String, String>>(), XLiveSearchFilters().params)
        assertEquals(true, XLiveSearchFilters().isEmpty)
        assertEquals(false, XLiveSearchFilters(price = XLivePrivatePrice.PRICE_6).isEmpty)
    }
}
