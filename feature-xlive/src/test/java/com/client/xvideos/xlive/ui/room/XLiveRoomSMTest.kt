package com.client.xvideos.xlive.ui.room

import com.client.xvideos.xlive.model.XLiveBio
import com.client.xvideos.xlive.model.XLiveRoom
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.saved.testRoomStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

// Без runTest: загрузчики отвечают сразу, а неограниченный диспетчер гонит
// корутины screenModelScope до конца прямо внутри вызова.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveRoomSMTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val saved by lazy { testRoomStore(tmp.newFolder("my")) }
    private val history by lazy { testRoomStore(tmp.newFolder("history")) }

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
        bio: suspend (String) -> XLiveBio = { XLiveBio(realName = "Anna of $it") },
    ) = XLiveRoomSM(
        room = "room_a",
        loadRoom = { name ->
            roomCalls++
            rooms[name] ?: throw IOException("нет комнаты")
        },
        loadBio = bio,
        nowMs = { nowMs },
        saved = saved,
        history = history,
    )

    @Test
    fun `открытая комната — эфир и анкета`() {
        val sm = model()

        assertEquals(XLiveRoomState.Live(live), sm.state.value)
        assertEquals("Anna of room_a", sm.bio.value?.realName)
    }

    @Test
    fun `не в открытом эфире — статус комнаты, без анкеты`() {
        val sm = model(rooms = mapOf("room_a" to XLiveRoom("private", "")))

        assertEquals(XLiveRoomState.Offline("private"), sm.state.value)
        assertNull(sm.bio.value)
    }

    @Test
    fun `комната открывается без звука, кнопка его включает и выключает`() {
        val sm = model()
        assertEquals(true, sm.muted.value)

        sm.toggleMute()
        assertEquals(false, sm.muted.value)

        sm.toggleMute()
        assertEquals(true, sm.muted.value)
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
            loadBio = { XLiveBio(realName = "Anna") },
            nowMs = { nowMs },
            saved = saved,
            history = history,
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

    @Test
    fun `открытие пишет комнату в историю, анкета дописывает возраст`() {
        model(bio = { XLiveBio(age = 24) })

        assertEquals(listOf(XLiveSavedRoom("room_a", age = 24)), history.rooms.value)
    }

    @Test
    fun `в историю пишется и комната не в эфире`() {
        model(rooms = mapOf("room_a" to XLiveRoom("offline", "")))

        assertEquals(listOf("room_a"), history.rooms.value.map { it.username })
    }

    @Test
    fun `кнопка в шапке сохраняет в мои с возрастом и убирает оттуда`() {
        val sm = model(bio = { XLiveBio(age = 24) })
        assertFalse(sm.isSaved.value)

        sm.toggleSaved()
        assertTrue(sm.isSaved.value)
        assertEquals(listOf(XLiveSavedRoom("room_a", age = 24)), saved.rooms.value)

        sm.toggleSaved()
        assertFalse(sm.isSaved.value)
        assertEquals(emptyList<XLiveSavedRoom>(), saved.rooms.value)
    }

    @Test
    fun `комната, уже сохранённая, открывается отмеченной`() {
        saved.put(XLiveSavedRoom("room_a"))

        assertTrue(model().isSaved.value)
    }
}
