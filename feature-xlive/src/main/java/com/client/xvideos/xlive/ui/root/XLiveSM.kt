package com.client.xvideos.xlive.ui.root

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.common.ui.scroll.LazyListPosition
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.model.XLiveTagSort
import com.client.xvideos.xlive.network.XLiveApi
import com.client.xvideos.xlive.ui.feed.XLiveFeedState
import com.client.xvideos.xlive.ui.feed.XLiveRoomsFeed
import com.client.xvideos.xlive.ui.tags.XLiveTagsFeed
import com.client.xvideos.xlive.ui.tags.XLiveTagsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Корень раздела XLive: нижняя вкладка, пол, лента «Эфиры» с подборкой и
 * таблица «Теги» с порядком. Пол общий для обеих.
 *
 * Теги грузятся только при первом открытии их вкладки.
 *
 * @param loadPage источник страницы списка комнат; подменяется в тестах.
 * @param loadTags источник страницы таблицы тегов; подменяется в тестах.
 */
class XLiveSM(
    private val loadPage: suspend (XLiveGender, XLiveCollection, Int) -> XLiveRoomPage = { gender, collection, offset ->
        XLiveApi.rooms(gender, collection, offset)
    },
    private val loadTags: suspend (XLiveGender, XLiveTagSort, Int) -> XLiveTagPage = XLiveApi::tags,
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

    init {
        rooms.reload()
    }

    fun selectTab(tab: XLiveTab) {
        _tab.value = tab
        if (tab == XLiveTab.TAGS) tagsFeed.start()
    }

    /** Пол общий: лента перезапрашивается сразу, таблица тегов — если её уже открывали. */
    fun selectGender(gender: XLiveGender) {
        if (_gender.value == gender) return
        _gender.value = gender
        rooms.reload()
        if (tagsFeed.isStarted) tagsFeed.reload()
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
}
