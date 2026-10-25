package com.client.xvideos.xlive.ui.search.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.xlive.ui.feed.atom.XLivePlaceholder
import com.client.xvideos.xlive.ui.feed.molecule.XLiveRoomsGrid
import com.client.xvideos.xlive.ui.search.XLiveSearchActions
import com.client.xvideos.xlive.ui.search.XLiveSearchState

/**
 * Вкладка «Поиск»: шапка с полем и фильтрами, под ней результат той же
 * сеткой, что «Эфиры». Пока искать нечего — подсказка вместо сетки.
 */
@Composable
fun XLiveSearchContent(
    state: XLiveSearchState,
    gridState: LazyGridState,
    actions: XLiveSearchActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        XLiveSearchHeader(
            query = state.query,
            filters = state.filters,
            onQueryChange = actions.onQueryChange,
            onSearch = actions.onSearch,
            onClear = actions.onClear,
            onFilters = actions.onFilters,
        )
        if (state.isActive) {
            XLiveRoomsGrid(feed = state.feed, gridState = gridState, actions = actions.feed, modifier = Modifier.weight(1f))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                XLivePlaceholder(text = "Введите ник или слово из темы или выберите фильтр")
            }
        }
    }
}

@Preview
@Composable
private fun XLiveSearchContentPreview() {
    XLiveSearchContent(state = XLiveSearchState(), gridState = rememberLazyGridState(), actions = XLiveSearchActions())
}
