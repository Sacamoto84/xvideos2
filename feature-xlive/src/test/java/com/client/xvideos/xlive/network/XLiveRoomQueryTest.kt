package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
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
}
