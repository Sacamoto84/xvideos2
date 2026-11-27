package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.n.ui.model.LocalNGridScrollIndicator
import com.client.xvideos.n.ui.model.NGridScrollIndicatorLayout
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.NGridScrollIndicator(state: LazyGridState, columns: Int) {
    val visibleRange = rememberVisibleRangePercentIgnoringFirstNForGrid(
        state,
        numberOfColumns = adaptiveUserGridColumns(columns),
    )
    val host = LocalNGridScrollIndicator.current
    val track = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(2.dp)
    if (host == null) {
        Box(track) { VerticalScrollbar { visibleRange.value } }
    } else {
        DisposableEffect(host, visibleRange) {
            onDispose {
                if (host.value?.visibleRange === visibleRange) host.value = null
            }
        }
        Box(track.onGloballyPositioned { coordinates ->
            val topInRoot = coordinates.positionInRoot().y.roundToInt()
            val height = coordinates.size.height
            val current = host.value
            if (current?.visibleRange !== visibleRange ||
                current.topInRoot != topInRoot ||
                current.height != height
            ) {
                host.value = NGridScrollIndicatorLayout(
                    visibleRange = visibleRange,
                    topInRoot = topInRoot,
                    height = height,
                )
            }
        })
    }
}

@Preview
@Composable
private fun NGridScrollIndicatorPreview() {
    Box(Modifier.fillMaxSize()) {
        NGridScrollIndicator(state = rememberLazyGridState(), columns = 2)
    }
}
