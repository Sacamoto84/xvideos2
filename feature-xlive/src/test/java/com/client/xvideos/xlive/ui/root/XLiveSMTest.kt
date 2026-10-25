package com.client.xvideos.xlive.ui.root

import com.client.xvideos.xlive.auth.TestAccount
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveFollowed
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.model.XLiveAgeRange
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.model.XLiveSearchFilters
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

    private data class SearchRequest(
        val gender: XLiveGender,
        val keywords: String,
        val filters: XLiveSearchFilters,
        val offset: Int,
    )

    private val searchRequests = mutableListOf<SearchRequest>()

    private data class Request(val gender: XLiveGender, val collection: XLiveCollection, val offset: Int)

    private data class TagRequest(val gender: XLiveGender, val sort: XLiveTagSort, val page: Int)

    private val requests = mutableListOf<Request>()
    private var pages: (Request) -> XLiveRoomPage = { page(it.offset, "a", "b") }
    private val tagRequests = mutableListOf<TagRequest>()
    private val account = TestAccount()
    private var followedLoads = 0
    private val followCalls = mutableListOf<Pair<String, Boolean>>()
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
        loadSearch = { gender, keywords, filters, offset ->
            searchRequests += SearchRequest(gender, keywords, filters, offset)
            page(offset, "s")
        },
        saved = saved,
        history = history,
        isOnline = { name ->
            onlineAsked += name
            name in onlineRooms
        },
        nowSeconds = { nowSeconds },
        account = account,
        loadFollowed = {
            followedLoads++
            XLiveFollowed(online = listOf(XLiveRoomItem("f1")), offline = listOf(XLiveRoomItem("f2")))
        },
        setFollowing = { name, follow ->
            followCalls += name to follow
            follow
        },
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

    @Test
    fun `Подписки грузятся при открытии подвкладки, эфир сохранённых при этом не проверяется`() {
        saved.put(XLiveSavedRoom("a"))
        val sm = model()
        sm.selectTab(XLiveTab.SAVED)
        assertEquals(0, followedLoads)
        onlineAsked.clear()

        sm.selectSavedTab(XLiveSavedTab.FOLLOWING)

        assertEquals(1, followedLoads)
        assertEquals(emptyList<String>(), onlineAsked)
        assertEquals(listOf("f1", "f2"), sm.following.value.list.rooms.map { it.username })
    }

    @Test
    fun `свайп вниз и убрать в Подписках — с сайта, перечитать и отписаться`() {
        val sm = model()
        sm.selectTab(XLiveTab.SAVED)
        sm.selectSavedTab(XLiveSavedTab.FOLLOWING)

        sm.refreshSaved()
        assertEquals(2, followedLoads)

        sm.removeSaved("f1")
        assertEquals(listOf("f1" to false), followCalls)
    }

    @Test
    fun `Выйти — выход аккаунта, Подписки пустые`() {
        val sm = model()
        sm.selectTab(XLiveTab.SAVED)
        sm.selectSavedTab(XLiveSavedTab.FOLLOWING)

        sm.signOut()

        assertEquals(1, account.signOuts)
        assertFalse(sm.following.value.isSignedIn)
        assertEquals(emptyList<String>(), sm.following.value.list.rooms.map { it.username })
    }

    @Test
    fun `поиск не идёт, пока нет слова или фильтра`() {
        val sm = model()

        sm.changeSearchQuery("  ")
        sm.submitSearch()

        assertEquals(emptyList<SearchRequest>(), searchRequests)
        assertFalse(sm.search.value.isActive)
    }

    @Test
    fun `Найти — слово без пробелов по краям, результат в сетке`() {
        val sm = model()

        sm.changeSearchQuery(" anna ")
        sm.submitSearch()

        assertEquals(listOf(SearchRequest(XLiveGender.ALL, "anna", XLiveSearchFilters(), 0)), searchRequests)
        assertTrue(sm.search.value.isActive)
        assertEquals(listOf("s0"), sm.search.value.feed.rooms.map { it.username })
        assertEquals(" anna ", sm.search.value.query)
    }

    @Test
    fun `набор текста без Найти не перезапрашивает`() {
        val sm = model()
        sm.changeSearchQuery("anna")
        sm.submitSearch()

        sm.changeSearchQuery("annabel")

        assertEquals(1, searchRequests.size)
    }

    @Test
    fun `фильтр без слова запускает поиск, смена фильтра — заново`() {
        val sm = model()
        val adults = XLiveSearchFilters(age = XLiveAgeRange.AGE_30_50)

        sm.selectSearchFilters(adults)
        sm.selectSearchFilters(adults)

        assertEquals(listOf(SearchRequest(XLiveGender.ALL, "", adults, 0)), searchRequests)
        assertTrue(sm.search.value.isActive)
    }

    @Test
    fun `крестик — слово уходит, фильтры остаются и ищут дальше`() {
        val sm = model()
        val adults = XLiveSearchFilters(age = XLiveAgeRange.AGE_30_50)
        sm.changeSearchQuery("anna")
        sm.submitSearch()
        sm.selectSearchFilters(adults)

        sm.clearSearch()

        assertEquals("", sm.search.value.query)
        assertEquals(SearchRequest(XLiveGender.ALL, "", adults, 0), searchRequests.last())
        assertTrue(sm.search.value.isActive)

        sm.selectSearchFilters(XLiveSearchFilters())
        assertFalse(sm.search.value.isActive)
    }

    @Test
    fun `пол перезапрашивает поиск, только если он идёт`() {
        val sm = model()
        sm.selectGender(XLiveGender.MALE)
        assertEquals(emptyList<SearchRequest>(), searchRequests)

        sm.changeSearchQuery("anna")
        sm.submitSearch()
        sm.selectGender(XLiveGender.FEMALE)

        assertEquals(SearchRequest(XLiveGender.FEMALE, "anna", XLiveSearchFilters(), 0), searchRequests.last())
    }

    @Test
    fun `догрузка поиска — следующий сдвиг с тем же словом`() {
        val sm = model()
        sm.changeSearchQuery("anna")
        sm.submitSearch()

        sm.loadMoreSearch()

        assertEquals(SearchRequest(XLiveGender.ALL, "anna", XLiveSearchFilters(), ROOM_PAGE_SIZE), searchRequests.last())
        assertEquals(listOf("s0", "s$ROOM_PAGE_SIZE"), sm.search.value.feed.rooms.map { it.username })
    }
}
