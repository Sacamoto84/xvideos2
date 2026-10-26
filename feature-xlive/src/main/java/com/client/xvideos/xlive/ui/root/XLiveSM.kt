package com.client.xvideos.xlive.ui.root

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.common.ui.scroll.LazyListPosition
import com.client.xvideos.xlive.auth.XLiveAccount
import com.client.xvideos.xlive.auth.XLiveSession
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveFollowed
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.model.XLiveSearchFilters
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.model.XLiveTagSort
import com.client.xvideos.xlive.network.XLiveApi
import com.client.xvideos.xlive.record.XLIVE_FRAME_VERSION
import com.client.xvideos.xlive.record.XLiveRecordStore
import com.client.xvideos.xlive.record.XLiveRecordings
import com.client.xvideos.xlive.saved.XLiveRoomStore
import com.client.xvideos.xlive.saved.XLiveSaved
import com.client.xvideos.xlive.ui.feed.XLiveFeedState
import com.client.xvideos.xlive.ui.feed.XLiveRoomsFeed
import com.client.xvideos.xlive.ui.following.XLiveFollowingList
import com.client.xvideos.xlive.ui.following.XLiveFollowingState
import com.client.xvideos.xlive.ui.saved.XLiveOnlineCheck
import com.client.xvideos.xlive.ui.saved.XLiveSavedState
import com.client.xvideos.xlive.ui.saved.XLiveRecordsState
import com.client.xvideos.xlive.ui.saved.XLiveSavedTab
import com.client.xvideos.xlive.ui.search.XLiveSearchState
import com.client.xvideos.xlive.ui.tags.XLiveTagsFeed
import com.client.xvideos.xlive.ui.tags.XLiveTagsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Корень раздела XLive: нижняя вкладка, пол, лента «Эфиры» с подборкой,
 * таблица «Теги» с порядком, «Избранное» с подвкладками и «Поиск» с
 * фильтрами. Пол общий для «Эфиров», «Тегов» и «Поиска».
 *
 * Теги грузятся только при первом открытии их вкладки, эфир сохранённых
 * комнат проверяется, когда открыто «Избранное», подписки — когда открыты
 * «Подписки» и есть вход, поиск идёт, когда есть отправленное слово или фильтр.
 *
 * @param loadPage источник страницы списка комнат; подменяется в тестах.
 * @param loadTags источник страницы таблицы тегов; подменяется в тестах.
 * @param loadSearch источник страницы поиска: пол, слово, фильтры, сдвиг; подменяется в тестах.
 * @param saved «Мои»; в тестах — хранилище во временной папке.
 * @param history «История»; в тестах — хранилище во временной папке.
 * @param records «Записи»; в тестах — хранилище во временной папке.
 * @param finishPendingRecords доклейка записей, оборванных вместе с процессом; подменяется в тестах.
 * @param isOnline источник статуса эфира по нику; подменяется в тестах.
 * @param nowSeconds часы для метки снимков; подменяются в тестах.
 * @param account вход на сайт; подменяется в тестах.
 * @param loadFollowed подписки аккаунта; подменяются в тестах.
 * @param setFollowing подписка и отписка по нику; подменяются в тестах.
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
    private val records: XLiveRecordStore = XLiveRecordings.store,
    private val finishPendingRecords: () -> Unit = XLiveRecordings::finishPending,
    isOnline: suspend (String) -> Boolean = XLiveApi::isOnline,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / MILLIS_PER_SECOND },
    private val account: XLiveAccount = XLiveSession,
    loadFollowed: suspend () -> XLiveFollowed = XLiveApi::followed,
    setFollowing: suspend (String, Boolean) -> Boolean = XLiveApi::setFollowing,
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

    /** «Записи»: от новых к старым. */
    val recordsState: StateFlow<XLiveRecordsState> = records.records
        .map(::recordsStateOf)
        .stateIn(screenModelScope, SharingStarted.Eagerly, recordsStateOf(records.records.value))

    val recordsPosition = LazyGridPosition()

    private val followingList = XLiveFollowingList(screenModelScope, account, loadFollowed, setFollowing, nowSeconds)

    /** «Подписки»: вход, список с сайта, ошибки. */
    val following: StateFlow<XLiveFollowingState> get() = followingList.state
    val followingPosition: LazyGridPosition get() = followingList.gridPosition

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

    /** Свайп вниз в «Избранном»: эфир заново у всех, свежие снимки; «Подписки» — с сайта заново. */
    fun refreshSaved() {
        if (_savedTab.value == XLiveSavedTab.FOLLOWING) {
            followingList.refresh()
            return
        }
        if (_savedTab.value == XLiveSavedTab.RECORDS) {
            records.refresh()
            return
        }
        snapshotStamp.value = nowSeconds()
        checkSaved(force = true)
    }

    /** Убрать комнату [username] из показанной подвкладки; из «Подписок» — отписаться на сайте. */
    fun removeSaved(username: String) {
        when (_savedTab.value) {
            XLiveSavedTab.MY -> saved.remove(username)
            XLiveSavedTab.HISTORY -> history.remove(username)
            XLiveSavedTab.FOLLOWING -> followingList.unfollow(username)
            // Записи удаляются по id — removeRecord.
            XLiveSavedTab.RECORDS -> Unit
        }
    }

    private fun recordsStateOf(list: ImmutableList<XLiveRecord>) = XLiveRecordsState(
        records = list,
        // Картинка карточки — кадр начала из видео; пока кадра нынешнего способа
        // нет — снимок сайта. Путь меняется, когда кадр готов, и карточка
        // загружает его заново: запрос картинки помнится по пути.
        snapshots = list.associate {
            val frameReady = it.hasFrame && it.frameVersion == XLIVE_FRAME_VERSION
            val name = if (frameReady) XLiveRecordStore.FRAME else XLiveRecordStore.SNAPSHOT
            it.id to File(records.dirOf(it.id), name).absolutePath
        }.toImmutableMap(),
    )

    /** Удалить запись [id] вместе с её файлами. */
    fun removeRecord(id: Long) {
        records.remove(id)
    }

    fun retryFollowing() = followingList.retry()

    /** «Выйти» в «Подписках». */
    fun signOut() = account.signOut()

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
            XLiveSavedTab.FOLLOWING -> {
                followingList.show()
                return
            }
            XLiveSavedTab.RECORDS -> {
                // Эфир у записей не проверяется; оборванные — доклеиваются.
                finishPendingRecords()
                return
            }
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
