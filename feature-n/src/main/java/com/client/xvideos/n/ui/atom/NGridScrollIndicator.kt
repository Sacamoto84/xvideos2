package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid

@Composable
internal fun BoxScope.NGridScrollIndicator(state: LazyGridState, columns: Int) {
    val visibleRange = rememberVisibleRangePercentIgnoringFirstNForGrid(
        state,
        numberOfColumns = adaptiveUserGridColumns(columns),
    )
    Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(2.dp)) {
        VerticalScrollbar { visibleRange.value }
    }
}
