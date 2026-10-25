package com.client.xvideos.xlive.ui.search

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.model.XLiveSearchFilters
import com.client.xvideos.xlive.ui.feed.XLiveFeedState

/**
 * Вкладка «Поиск».
 *
 * @property query текст в поле, ещё не обязательно отправленный.
 * @property filters выбранные фильтры.
 * @property isActive поиск запущен: есть отправленное слово или фильтр. Иначе вместо сетки подсказка.
 * @property feed результат — та же лента, что у «Эфиров».
 */
@Immutable
data class XLiveSearchState(
    val query: String = "",
    val filters: XLiveSearchFilters = XLiveSearchFilters(),
    val isActive: Boolean = false,
    val feed: XLiveFeedState = XLiveFeedState(),
)
