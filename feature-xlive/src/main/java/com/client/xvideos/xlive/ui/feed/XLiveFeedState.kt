package com.client.xvideos.xlive.ui.feed

import com.client.xvideos.xlive.model.XLiveRoomItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Лента вкладки «Эфиры».
 *
 * @property rooms загруженные комнаты без повторов, в порядке сайта.
 * @property isLoading грузится первая страница, комнат на экране нет.
 * @property isRefreshing обновление свайпом вниз: старые комнаты на месте, пока не пришли новые.
 * @property isLoadingMore догружается следующая страница.
 * @property error первая страница не пришла.
 * @property loadMoreError следующая страница не пришла.
 * @property isEndReached дальше комнат нет.
 */
data class XLiveFeedState(
    val rooms: ImmutableList<XLiveRoomItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val loadMoreError: String? = null,
    val isEndReached: Boolean = false,
)
