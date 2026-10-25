package com.client.xvideos.xlive.ui.tags

import com.client.xvideos.xlive.model.XLiveTag
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Таблица вкладки «Теги».
 *
 * @property tags загруженные теги без повторов, в порядке сайта.
 * @property isLoading грузится первая страница, тегов на экране нет.
 * @property isLoadingMore догружается следующая страница.
 * @property error первая страница не пришла.
 * @property loadMoreError следующая страница не пришла.
 * @property isEndReached дальше тегов нет.
 */
data class XLiveTagsState(
    val tags: ImmutableList<XLiveTag> = persistentListOf(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val loadMoreError: String? = null,
    val isEndReached: Boolean = false,
)
