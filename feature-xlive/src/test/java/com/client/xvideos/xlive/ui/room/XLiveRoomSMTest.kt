package com.client.xvideos.xlive.ui.room

import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

// Без runTest: загрузчики отвечают сразу, а неограниченный диспетчер гонит
// корутины screenModelScope до конца прямо внутри вызова.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveRoomSMTest {

    private val live = XLiveRoom(status = "public", hlsUrl = "https://edge.test/a.m3u8", username = "room_a")
    private var nowMs = 0L
    private var roomCalls = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(
        rooms: Map<String, XLiveRoom> = mapOf("room_a" to live),
        publicRooms: List<String> = emptyList(),
        bio: suspend (String) -> XLiveBio = { XLiveBio(realName = "Anna of $it") },
    ) = XLiveRoomSM(
        room = "room_a",
        loadRoom = { name ->
            roomCalls++
            rooms[name] ?: throw IOException("нет комнаты")
        },
        loadPublicRooms = { publicRooms },
        loadBio = bio,
        nowMs = { nowMs },
    )

    @Test
    fun `открытая комната — эфир и анкета`() {
        val sm = model()

        assertEquals(XLiveRoomState.Live(live), sm.state.value)
        assertEquals("Anna of room_a", sm.bio.value?.realName)
    }

    @Test
    fun `комната в привате — первая открытая из списка, со статусом запрошенной`() {
        val other = XLiveRoom(status = "public", hlsUrl = "https://edge.test/b.m3u8")
        val sm = model(
            rooms = mapOf("room_a" to XLiveRoom("private", ""), "room_b" to other),
            publicRooms = listOf("room_a", "room_b"),
        )

        assertEquals(XLiveRoomState.Live(other.copy(username = "room_b"), requestedStatus = "private"), sm.state.value)
        assertEquals("Anna of room_b", sm.bio.value?.realName)
    }

    @Test
    fun `замены нет — офлайн со статусом комнаты`() {
        val sm = model(rooms = mapOf("room_a" to XLiveRoom("offline", "")))

        assertEquals(XLiveRoomState.Offline("offline"), sm.state.value)
        assertNull(sm.bio.value)
    }

    @Test
    fun `нет сети — ошибка с классом исключения`() {
        assertEquals(XLiveRoomState.Failed("IOException"), model(rooms = emptyMap()).state.value)
    }

    @Test
    fun `анкета не пришла — эфир без анкеты`() {
        val sm = model(bio = { throw IOException("нет анкеты") })

        assertEquals(XLiveRoomState.Live(live), sm.state.value)
        assertNull(sm.bio.value)
    }

    @Test
    fun `ошибка плеера сразу после загрузки — показывается, повтор той же ошибки не в счёт`() {
        val sm = model()
        sm.onPlayerStarted(live.hlsUrl)
        nowMs += 5_000

        sm.onPlayerError("Source error")
        sm.onPlayerError("Source error")

        assertEquals(XLiveRoomState.Failed("Source error"), sm.state.value)
        assertEquals(1, roomCalls)
    }

    @Test
    fun `ошибка плеера после долгого эфира — молча свежий адрес, повтор той же ошибки не в счёт`() {
        val sm = model()
        sm.onPlayerStarted(live.hlsUrl)
        nowMs += 60_000

        sm.onPlayerError("Source error")
        sm.onPlayerError("Source error")

        assertEquals(XLiveRoomState.Live(live), sm.state.value)
        assertEquals(2, roomCalls)
    }

    @Test
    fun `ошибка до старта плеера — не в счёт`() {
        val sm = model()

        sm.onPlayerError("Source error")

        assertEquals(XLiveRoomState.Live(live), sm.state.value)
    }

    @Test
    fun `первый старт плеера — без запроса, повторный с тем же адресом — свежий адрес без экрана загрузки`() {
        var sm: XLiveRoomSM? = null
        val seenDuringLoad = mutableListOf<XLiveRoomState?>()
        sm = XLiveRoomSM(
            room = "room_a",
            loadRoom = {
                roomCalls++
                seenDuringLoad += sm?.state?.value
                live
            },
            loadPublicRooms = { emptyList() },
            loadBio = { XLiveBio(realName = "Anna") },
            nowMs = { nowMs },
        )

        sm.onPlayerStarted(live.hlsUrl)
        assertEquals(1, roomCalls)

        sm.onPlayerStarted(live.hlsUrl)

        assertEquals(2, roomCalls)
        assertEquals(XLiveRoomState.Live(live.copy(hlsUrl = "")), seenDuringLoad.last())
        assertEquals(XLiveRoomState.Live(live), sm.state.value)
        assertEquals("Anna", sm.bio.value?.realName)
    }

    @Test
    fun `старт с адресом, которого нет в состоянии, — не в счёт`() {
        val sm = model()

        sm.onPlayerStarted("https://edge.test/old.m3u8")
        sm.onPlayerStarted("https://edge.test/old.m3u8")

        assertEquals(1, roomCalls)
    }

    @Test
    fun `повтор той же комнаты не сбрасывает анкету и развёрнутое о себе`() {
        var bioCalls = 0
        val sm = model(bio = { bioCalls++; XLiveBio(realName = "Anna") })
        sm.toggleAbout()

        sm.reload()

        assertEquals(1, bioCalls)
        assertEquals("Anna", sm.bio.value?.realName)
        assertEquals(true, sm.aboutExpanded.value)
    }
}
