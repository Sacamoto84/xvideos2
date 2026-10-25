package com.client.xvideos.xlive.ui.search

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.model.XLiveSearchFilters
import com.client.xvideos.xlive.ui.feed.XLiveFeedActions

/**
 * Действия вкладки «Поиск» — пара к [XLiveSearchState].
 *
 * @property onQueryChange набор текста в поле.
 * @property onSearch «Найти» на клавиатуре или лупа в поле.
 * @property onClear крестик в поле: текст и результат по нему уходят, фильтры остаются.
 * @property onFilters выбран фильтр.
 * @property feed действия сетки результата.
 */
@Immutable
data class XLiveSearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onFilters: (XLiveSearchFilters) -> Unit = {},
    val feed: XLiveFeedActions = XLiveFeedActions(),
)
