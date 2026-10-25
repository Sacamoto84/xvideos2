package com.client.xvideos.xlive.ui.tag

import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.network.ROOM_PAGE_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Без runTest: загрузчик отвечает сразу, а неограниченный диспетчер гонит
// корутины screenModelScope до конца прямо внутри вызова.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveTagSMTest {

    private val requests = mutableListOf<Triple<XLiveGender, String, Int>>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `лента тега — пол и тег экрана, страницы со сдвигом`() {
        val sm = XLiveTagSM(tag = "petite", gender = XLiveGender.FEMALE, loadPage = { gender, tag, offset ->
            requests += Triple(gender, tag, offset)
            XLiveRoomPage(listOf(XLiveRoomItem("room$offset")), total = 1000)
        })

        sm.loadMore()

        assertEquals(
            listOf(Triple(XLiveGender.FEMALE, "petite", 0), Triple(XLiveGender.FEMALE, "petite", ROOM_PAGE_SIZE)),
            requests
        )
        assertEquals(listOf("room0", "room$ROOM_PAGE_SIZE"), sm.feed.value.rooms.map { it.username })
    }
}
