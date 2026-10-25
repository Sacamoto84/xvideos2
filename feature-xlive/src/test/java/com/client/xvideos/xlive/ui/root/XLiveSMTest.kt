package com.client.xvideos.xlive.ui.root

import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.model.XLiveTag
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.model.XLiveTagSort
import com.client.xvideos.xlive.network.ROOM_PAGE_SIZE
import com.client.xvideos.xlive.network.TAG_PAGE_SIZE
import com.client.xvideos.xlive.saved.testRoomStore
import com.client.xvideos.xlive.ui.saved.XLiveSavedTab
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

// Без runTest: загрузчик отвечает сразу, а неограниченный диспетчер гонит
// корутины screenModelScope до конца прямо внутри вызова.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveSMTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private var clock = 0L
    private val saved by lazy { testRoomStore(tmp.newFolder("my"), now = { ++clock }) }
    private val history by lazy { testRoomStore(tmp.newFolder("history"), now = { ++clock }) }
    private val onlineAsked = mutableListOf<String>()
    private var onlineRooms = setOf<String>()
    private var nowSeconds = 1_000L

    private data class Request(val gender: XLiveGender, val collection: XLiveCollection, val offset: Int)

    private data class TagRequest(val gender: XLiveGender, val sort: XLiveTagSort, val page: Int)

    private val requests = mutableListOf<Request>()
    private var pages: (Request) -> XLiveRoomPage = { page(it.offset, "a", "b") }
    private val tagRequests = mutableListOf<TagRequest>()
    private var tagPages: (TagRequest) -> XLiveTagPage = { XLiveTagPage(listOf(XLiveTag("t${it.page}", 1, 1)), 1000) }

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

    private fun model() = XLiveSM(
        loadPage = { gender, collection, offset ->
            val request = Request(gender, collection, offset)
            requests += request
            pages(request)
        },
        loadTags = { gender, sort, page ->
            val request = TagRequest(gender, sort, page)
            tagRequests += request
            tagPages(request)
        },
        saved = saved,
        history = history,
        isOnline = { name ->
            onlineAsked += name
            name in onlineRooms
        },
        nowSeconds = { nowSeconds },
    )

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
    fun `вкладка меняется без запросов эфиров`() {
        val sm = model()

        sm.selectTab(XLiveTab.TAGS)

        assertEquals(XLiveTab.TAGS, sm.tab.value)
        assertEquals(1, requests.size)
    }

    @Test
    fun `теги — только при первом открытии вкладки, повторное открытие без запроса`() {
        val sm = model()
        assertEquals(emptyList<TagRequest>(), tagRequests)

        sm.selectTab(XLiveTab.TAGS)
        sm.selectTab(XLiveTab.ROOMS)
        sm.selectTab(XLiveTab.TAGS)

        assertEquals(listOf(TagRequest(XLiveGender.ALL, XLiveTagSort.VIEWERS, 1)), tagRequests)
        assertEquals(listOf("t1"), sm.tags.value.tags.map { it.name })
    }

    @Test
    fun `теги — догрузка следующей страницы без повторов`() {
        tagPages = { XLiveTagPage(listOf(XLiveTag("same", 1, 1), XLiveTag("t${it.page}", 1, 1)), 1000) }
        val sm = model()
        sm.selectTab(XLiveTab.TAGS)

        sm.loadMoreTags()

        assertEquals(2, tagRequests.last().page)
        assertEquals(listOf("same", "t1", "t2"), sm.tags.value.tags.map { it.name })
    }

    @Test
    fun `теги — конец, когда страницы покрыли total`() {
        tagPages = { XLiveTagPage(listOf(XLiveTag("a", 1, 1)), total = TAG_PAGE_SIZE) }
        val sm = model()
        sm.selectTab(XLiveTab.TAGS)

        assertTrue(sm.tags.value.isEndReached)
        sm.loadMoreTags()
        assertEquals(1, tagRequests.size)
    }

    @Test
    fun `порядок тегов — таблица с нуля`() {
        val sm = model()
        sm.selectTab(XLiveTab.TAGS)

        sm.selectTagSort(XLiveTagSort.NAME)

        assertEquals(TagRequest(XLiveGender.ALL, XLiveTagSort.NAME, 1), tagRequests.last())
        assertEquals(XLiveTagSort.NAME, sm.tagSort.value)
    }

    @Test
    fun `пол — теги перезапрашиваются, только если их уже открывали`() {
        val sm = model()
        sm.selectGender(XLiveGender.MALE)
        assertEquals("теги не открывали — запроса нет", emptyList<TagRequest>(), tagRequests)

        sm.selectTab(XLiveTab.TAGS)
        sm.selectGender(XLiveGender.TRANS)

        assertEquals(TagRequest(XLiveGender.TRANS, XLiveTagSort.VIEWERS, 1), tagRequests.last())
    }

    @Test
    fun `теги не пришли — ошибка, Повторить грузит снова`() {
        tagPages = { throw IOException("нет сети") }
        val sm = model()
        sm.selectTab(XLiveTab.TAGS)
        assertEquals("Не удалось загрузить теги", sm.tags.value.error)

        tagPages = { XLiveTagPage(listOf(XLiveTag("a", 1, 1)), 1000) }
        sm.reloadTags()

        assertNull(sm.tags.value.error)
        assertEquals(listOf("a"), sm.tags.value.tags.map { it.name })
    }

    private fun XLiveSM.myNames() = my.value.rooms.map { it.username }

    @Test
    fun `эфир сохранённых не проверяется, пока Избранное не открыли`() {
        saved.put(XLiveSavedRoom("a"))
        model()

        assertEquals(emptyList<String>(), onlineAsked)
    }

    @Test
    fun `Мои — сначала в эфире, внутри — от недавно сохранённых`() {
        listOf("a", "b", "c", "d").forEach { saved.put(XLiveSavedRoom(it)) }
        onlineRooms = setOf("a", "c")
        val sm = model()

        sm.selectTab(XLiveTab.SAVED)

        assertEquals(listOf("d", "c", "b", "a"), onlineAsked)
        assertEquals(listOf("c", "a", "d", "b"), sm.myNames())
        assertEquals(mapOf("a" to true, "b" to false, "c" to true, "d" to false), sm.my.value.online)
    }

    @Test
    fun `История — от недавних, проверяется при переходе на неё`() {
        listOf("a", "b").forEach { history.put(XLiveSavedRoom(it)) }
        onlineRooms = setOf("a")
        val sm = model()
        sm.selectTab(XLiveTab.SAVED)

        sm.selectSavedTab(XLiveSavedTab.HISTORY)

        assertEquals(listOf("b", "a"), onlineAsked)
        assertEquals(listOf("b", "a"), sm.recent.value.rooms.map { it.username })
    }

    @Test
    fun `комната, сохранённая при открытом Избранном, проверяется сразу`() {
        val sm = model()
        sm.selectTab(XLiveTab.SAVED)

        saved.put(XLiveSavedRoom("a"))

        assertEquals(listOf("a"), onlineAsked)
        assertEquals(false, sm.my.value.online["a"])
    }

    @Test
    fun `свайп вниз — эфир заново у всех и свежие снимки`() {
        saved.put(XLiveSavedRoom("a"))
        val sm = model()
        sm.selectTab(XLiveTab.SAVED)
        assertEquals(1_000L, sm.my.value.snapshotStamp)

        nowSeconds = 1_090L
        onlineRooms = setOf("a")
        sm.refreshSaved()

        assertEquals(listOf("a", "a"), onlineAsked)
        assertEquals(true, sm.my.value.online["a"])
        assertEquals(1_090L, sm.my.value.snapshotStamp)
    }

    @Test
    fun `убрать — из показанной подвкладки`() {
        saved.put(XLiveSavedRoom("a"))
        history.put(XLiveSavedRoom("a"))
        val sm = model()

        sm.removeSaved("a")
        assertEquals(emptyList<String>(), sm.myNames())
        assertEquals(listOf("a"), sm.recent.value.rooms.map { it.username })

        sm.selectSavedTab(XLiveSavedTab.HISTORY)
        sm.removeSaved("a")
        assertEquals(emptyList<String>(), sm.recent.value.rooms.map { it.username })
    }
}
