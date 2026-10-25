package com.client.xvideos.xlive.ui.root

import com.client.xvideos.xlive.model.XLiveCollection
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// Без runTest: загрузчик отвечает сразу, а неограниченный диспетчер гонит
// корутины screenModelScope до конца прямо внутри вызова.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveSMTest {

    private data class Request(val gender: XLiveGender, val collection: XLiveCollection, val offset: Int)

    private val requests = mutableListOf<Request>()
    private var pages: (Request) -> XLiveRoomPage = { page(it.offset, "a", "b") }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun page(offset: Int, vararg names: String, total: Int = 1000) =
        XLiveRoomPage(names.map { XLiveRoomItem(username = "$it$offset") }, total)

    private fun model() = XLiveSM(loadPage = { gender, collection, offset ->
        val request = Request(gender, collection, offset)
        requests += request
        pages(request)
    })

    private fun XLiveSM.names() = feed.value.rooms.map { it.username }

    @Test
    fun `старт — первая страница всех популярных`() {
        val sm = model()

        assertEquals(listOf(Request(XLiveGender.ALL, XLiveCollection.POPULAR, 0)), requests)
        assertEquals(listOf("a0", "b0"), sm.names())
        assertFalse(sm.feed.value.isLoading)
        assertFalse(sm.feed.value.isEndReached)
    }

    @Test
    fun `догрузка — следующий сдвиг, повторы по нику отброшены`() {
        pages = { if (it.offset == 0) page(0, "a", "b") else XLiveRoomPage(listOf(XLiveRoomItem("b0"), XLiveRoomItem("c")), 1000) }
        val sm = model()

        sm.loadMore()

        assertEquals(ROOM_PAGE_SIZE, requests.last().offset)
        assertEquals(listOf("a0", "b0", "c"), sm.names())
    }

    @Test
    fun `конец — страница только с промо или сдвиг дошёл до total`() {
        pages = { XLiveRoomPage(listOf(XLiveRoomItem("promo", isPromoted = true)), 1000) }
        assertTrue(model().feed.value.isEndReached)

        pages = { page(it.offset, "a", total = ROOM_PAGE_SIZE) }
        val sm = model()
        assertTrue(sm.feed.value.isEndReached)

        val before = requests.size
        sm.loadMore()
        assertEquals("после конца догрузки нет", before, requests.size)
    }

    @Test
    fun `смена пола и подборки — лента с нуля с новыми параметрами`() {
        val sm = model()
        sm.loadMore()

        sm.selectGender(XLiveGender.FEMALE)
        sm.selectCollection(XLiveCollection.NEW)

        assertEquals(Request(XLiveGender.FEMALE, XLiveCollection.NEW, 0), requests.last())
        assertEquals(listOf("a0", "b0"), sm.names())
        assertEquals(XLiveGender.FEMALE, sm.gender.value)
        assertEquals(XLiveCollection.NEW, sm.collection.value)
    }

    @Test
    fun `выбор того же пола — без запроса`() {
        val sm = model()

        sm.selectGender(XLiveGender.ALL)

        assertEquals(1, requests.size)
        assertEquals(2, sm.names().size)
    }

    @Test
    fun `первая страница не пришла — ошибка, Повторить грузит снова`() {
        pages = { throw IOException("нет сети") }
        val sm = model()
        assertEquals("Не удалось загрузить эфиры", sm.feed.value.error)

        sm.loadMore()
        assertEquals("без первой страницы догрузки нет", 1, requests.size)

        pages = { page(it.offset, "a") }
        sm.reload()

        assertNull(sm.feed.value.error)
        assertEquals(listOf("a0"), sm.names())
    }

    @Test
    fun `следующая страница не пришла — ошибка в подвале, повтор грузит тот же сдвиг`() {
        val sm = model()
        pages = { throw IOException("нет сети") }

        sm.loadMore()
        assertEquals("Не удалось загрузить ещё", sm.feed.value.loadMoreError)
        sm.loadMore()
        assertEquals("пока висит ошибка, догрузка сама не повторяется", 2, requests.size)

        pages = { page(it.offset, "c") }
        sm.retryLoadMore()

        assertEquals(ROOM_PAGE_SIZE, requests.last().offset)
        assertEquals(listOf("a0", "b0", "c$ROOM_PAGE_SIZE"), sm.names())
        assertNull(sm.feed.value.loadMoreError)
    }

    @Test
    fun `обновление свайпом — свежая первая страница вместо старой`() {
        val sm = model()
        sm.loadMore()
        pages = { page(it.offset, "fresh") }

        sm.refresh()

        assertEquals(listOf("fresh0"), sm.names())
        assertFalse(sm.feed.value.isRefreshing)
    }

    @Test
    fun `обновление не удалось — старые комнаты на месте`() {
        val sm = model()
        pages = { throw IOException("нет сети") }

        sm.refresh()

        assertEquals(listOf("a0", "b0"), sm.names())
        assertFalse(sm.feed.value.isRefreshing)
        assertNull(sm.feed.value.error)
    }

    @Test
    fun `вкладка меняется без запросов`() {
        val sm = model()

        sm.selectTab(XLiveTab.TAGS)

        assertEquals(XLiveTab.TAGS, sm.tab.value)
        assertEquals(1, requests.size)
    }
}
