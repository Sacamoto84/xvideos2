package com.client.xvideos.xlive.ui.root

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.common.ui.scroll.LazyListPosition
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.model.XLiveSearchFilters
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.model.XLiveTagSort
import com.client.xvideos.xlive.network.XLiveApi
import com.client.xvideos.xlive.saved.XLiveRoomStore
import com.client.xvideos.xlive.saved.XLiveSaved
import com.client.xvideos.xlive.ui.feed.XLiveFeedState
import com.client.xvideos.xlive.ui.feed.XLiveRoomsFeed
import com.client.xvideos.xlive.ui.saved.XLiveOnlineCheck
import com.client.xvideos.xlive.ui.saved.XLiveSavedState
import com.client.xvideos.xlive.ui.saved.XLiveSavedTab
import com.client.xvideos.xlive.ui.search.XLiveSearchState
import com.client.xvideos.xlive.ui.tags.XLiveTagsFeed
import com.client.xvideos.xlive.ui.tags.XLiveTagsState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Корень раздела XLive: нижняя вкладка, пол, лента «Эфиры» с подборкой,
 * таблица «Теги» с порядком, «Избранное» с подвкладками и «Поиск» с
 * фильтрами. Пол общий для «Эфиров», «Тегов» и «Поиска».
 *
 * Теги грузятся только при первом открытии их вкладки, эфир сохранённых
 * комнат проверяется, когда открыто «Избранное», поиск идёт, когда есть
 * отправленное слово или фильтр.
 *
 * @param loadPage источник страницы списка комнат; подменяется в тестах.
 * @param loadTags источник страницы таблицы тегов; подменяется в тестах.
 * @param loadSearch источник страницы поиска: пол, слово, фильтры, сдвиг; подменяется в тестах.
 * @param saved «Мои»; в тестах — хранилище во временной папке.
 * @param history «История»; в тестах — хранилище во временной папке.
 * @param isOnline источник статуса эфира по нику; подменяется в тестах.
 * @param nowSeconds часы для метки снимков; подменяются в тестах.
 */
class XLiveSM(
    private val loadPage: suspend (XLiveGender, XLiveCollection, Int) -> XLiveRoomPage = { gender, collection, offset ->
        XLiveApi.rooms(gender, collection, offset)
    },
    private val loadTags: suspend (XLiveGender, XLiveTagSort, Int) -> XLiveTagPage = XLiveApi::tags,
    private val loadSearch: suspend (XLiveGender, String, XLiveSearchFilters, Int) -> XLiveRoomPage =
        { gender, keywords, filters, offset ->
            XLiveApi.rooms(gender, XLiveCollection.POPULAR, offset, keywords = keywords, filters = filters)
        },
    private val saved: XLiveRoomStore = XLiveSaved.my,
    private val history: XLiveRoomStore = XLiveSaved.history,
    isOnline: suspend (String) -> Boolean = XLiveApi::isOnline,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / MILLIS_PER_SECOND },
) : ScreenModel {

    private val _tab = MutableStateFlow(XLiveTab.ROOMS)
    val tab: StateFlow<XLiveTab> = _tab.asStateFlow()

    private val _gender = MutableStateFlow(XLiveGender.ALL)
    val gender: StateFlow<XLiveGender> = _gender.asStateFlow()

    private val _collection = MutableStateFlow(XLiveCollection.POPULAR)
    val collection: StateFlow<XLiveCollection> = _collection.asStateFlow()

    private val _tagSort = MutableStateFlow(XLiveTagSort.VIEWERS)
    val tagSort: StateFlow<XLiveTagSort> = _tagSort.asStateFlow()

    private val rooms = XLiveRoomsFeed(
        scope = screenModelScope,
        loadPage = { offset -> loadPage(_gender.value, _collection.value, offset) },
        label = { "${_gender.value}/${_collection.value}" },
    )

    val feed: StateFlow<XLiveFeedState> get() = rooms.state
    val gridPosition: LazyGridPosition get() = rooms.gridPosition

    private val tagsFeed = XLiveTagsFeed(
        scope = screenModelScope,
        loadPage = { page -> loadTags(_gender.value, _tagSort.value, page) },
        label = { "${_gender.value}/${_tagSort.value}" },
    )

    val tags: StateFlow<XLiveTagsState> get() = tagsFeed.state
    val tagsPosition: LazyListPosition get() = tagsFeed.listPosition

    private val _savedTab = MutableStateFlow(XLiveSavedTab.MY)
    val savedTab: StateFlow<XLiveSavedTab> = _savedTab.asStateFlow()

    private val onlineCheck = XLiveOnlineCheck(screenModelScope, isOnline)
    private val snapshotStamp = MutableStateFlow(nowSeconds())

    /** «Мои»: сначала в эфире, внутри — от недавно сохранённых. */
    val my: StateFlow<XLiveSavedState> = savedState(saved) { rooms, online ->
        rooms.sortedByDescending { online[it.username] == true }
    }

    /** «История»: от недавно открытых, эфир — только плашкой. */
    val recent: StateFlow<XLiveSavedState> = savedState(history) { rooms, _ -> rooms }

    val myPosition = LazyGridPosition()
    val recentPosition = LazyGridPosition()

    private val searchQuery = MutableStateFlow("")
    private val searchFilters = MutableStateFlow(XLiveSearchFilters())

    /** Отправленное слово: поле может уже отличаться, лента — по нему. */
    private val searchKeywords = MutableStateFlow("")

    // Слово поиска в журнал не идёт — как и теги, это может быть что угодно.
    private val searchFeed = XLiveRoomsFeed(
        scope = screenModelScope,
        loadPage = { offset -> loadSearch(_gender.value, searchKeywords.value, searchFilters.value, offset) },
        label = { "${_gender.value}/поиск" },
    )

    val search: StateFlow<XLiveSearchState> =
        combine(searchQuery, searchFilters, searchKeywords, searchFeed.state) { query, filters, keywords, feed ->
            XLiveSearchState(
                query = query,
                filters = filters,
                isActive = keywords.isNotBlank() || !filters.isEmpty,
                feed = feed,
            )
        }.stateIn(screenModelScope, SharingStarted.Eagerly, XLiveSearchState())

    val searchPosition: LazyGridPosition get() = searchFeed.gridPosition

    private val isSearchActive: Boolean
        get() = searchKeywords.value.isNotBlank() || !searchFilters.value.isEmpty

    init {
        rooms.reload()
        // Комната, сохранённая или открытая, пока «Избранное» на экране, проверяется сразу.
        screenModelScope.launch {
            merge(saved.rooms, history.rooms).collect { if (_tab.value == XLiveTab.SAVED) checkSaved() }
        }
    }

    private fun savedState(
        store: XLiveRoomStore,
        order: (List<XLiveSavedRoom>, Map<String, Boolean>) -> List<XLiveSavedRoom>,
    ): StateFlow<XLiveSavedState> =
        combine(store.rooms, onlineCheck.online, onlineCheck.isChecking, snapshotStamp) { rooms, online, checking, stamp ->
            XLiveSavedState(
                rooms = order(rooms, online).toImmutableList(),
                online = online,
                isChecking = checking,
                snapshotStamp = stamp,
            )
        }.stateIn(screenModelScope, SharingStarted.Eagerly, XLiveSavedState(snapshotStamp = snapshotStamp.value))

    fun selectTab(tab: XLiveTab) {
        _tab.value = tab
        if (tab == XLiveTab.TAGS) tagsFeed.start()
        if (tab == XLiveTab.SAVED) checkSaved()
    }

    fun selectSavedTab(tab: XLiveSavedTab) {
        _savedTab.value = tab
        checkSaved()
    }

    /** Свайп вниз в «Избранном»: эфир заново у всех, свежие снимки. */
    fun refreshSaved() {
        snapshotStamp.value = nowSeconds()
        checkSaved(force = true)
    }

    /** Убрать комнату [username] из показанной подвкладки. */
    fun removeSaved(username: String) {
        when (_savedTab.value) {
            XLiveSavedTab.MY -> saved.remove(username)
            XLiveSavedTab.HISTORY -> history.remove(username)
            XLiveSavedTab.FOLLOWING -> Unit
        }
    }

    fun changeSearchQuery(text: String) {
        searchQuery.value = text
    }

    /** «Найти»: слово из поля уходит в поиск. Пустое слово без фильтров — снова подсказка. */
    fun submitSearch() {
        searchKeywords.value = searchQuery.value.trim()
        if (isSearchActive) searchFeed.reload()
    }

    /** Крестик: поле пустое, результат по слову уходит, фильтры остаются. */
    fun clearSearch() {
        searchQuery.value = ""
        if (searchKeywords.value.isEmpty()) return
        searchKeywords.value = ""
        if (isSearchActive) searchFeed.reload()
    }

    fun selectSearchFilters(filters: XLiveSearchFilters) {
        if (searchFilters.value == filters) return
        searchFilters.value = filters
        if (isSearchActive) searchFeed.reload()
    }

    fun reloadSearch() = searchFeed.reload()

    fun refreshSearch() = searchFeed.refresh()

    fun loadMoreSearch() = searchFeed.loadMore()

    fun retryLoadMoreSearch() = searchFeed.retryLoadMore()

    private fun checkSaved(force: Boolean = false) {
        val shown = when (_savedTab.value) {
            XLiveSavedTab.MY -> saved.rooms.value
            XLiveSavedTab.HISTORY -> history.rooms.value
            XLiveSavedTab.FOLLOWING -> return
        }
        onlineCheck.check(shown.map { it.username }, force)
    }

    /**
     * Пол общий: лента перезапрашивается сразу, таблица тегов — если её уже
     * открывали, поиск — если он идёт.
     */
    fun selectGender(gender: XLiveGender) {
        if (_gender.value == gender) return
        _gender.value = gender
        rooms.reload()
        if (tagsFeed.isStarted) tagsFeed.reload()
        if (isSearchActive) searchFeed.reload()
    }

    fun selectTagSort(sort: XLiveTagSort) {
        if (_tagSort.value == sort) return
        _tagSort.value = sort
        tagsFeed.reload()
    }

    fun selectCollection(collection: XLiveCollection) {
        if (_collection.value == collection) return
        _collection.value = collection
        rooms.reload()
    }

    fun reload() = rooms.reload()

    fun refresh() = rooms.refresh()

    fun loadMore() = rooms.loadMore()

    fun retryLoadMore() = rooms.retryLoadMore()

    fun reloadTags() = tagsFeed.reload()

    fun loadMoreTags() = tagsFeed.loadMore()

    fun retryLoadMoreTags() = tagsFeed.retryLoadMore()

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
