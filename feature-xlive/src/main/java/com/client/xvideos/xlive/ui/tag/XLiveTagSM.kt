package com.client.xvideos.xlive.ui.tag

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.network.XLiveApi
import com.client.xvideos.xlive.ui.feed.XLiveFeedState
import com.client.xvideos.xlive.ui.feed.XLiveRoomsFeed
import kotlinx.coroutines.flow.StateFlow

/**
 * Комнаты с тегом [tag] под полом [gender] — та же лента, что «Эфиры».
 *
 * Сам тег в журнал не идёт: теги — слова из тем комнат.
 *
 * @param loadPage источник страницы списка: пол, тег, сдвиг; подменяется в тестах.
 */
class XLiveTagSM(
    private val tag: String,
    private val gender: XLiveGender,
    private val loadPage: suspend (XLiveGender, String, Int) -> XLiveRoomPage = { g, t, offset ->
        XLiveApi.rooms(g, XLiveCollection.POPULAR, offset, hashtag = t)
    },
) : ScreenModel {

    private val rooms = XLiveRoomsFeed(
        scope = screenModelScope,
        loadPage = { offset -> loadPage(gender, tag, offset) },
        label = { "$gender/тег" },
    )

    val feed: StateFlow<XLiveFeedState> get() = rooms.state
    val gridPosition: LazyGridPosition get() = rooms.gridPosition

    init {
        rooms.reload()
    }

    fun reload() = rooms.reload()

    fun refresh() = rooms.refresh()

    fun loadMore() = rooms.loadMore()

    fun retryLoadMore() = rooms.retryLoadMore()
}
