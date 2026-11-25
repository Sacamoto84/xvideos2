package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import kotlin.math.roundToInt

internal data class NGridScrollIndicatorLayout(
    val visibleRange: State<Pair<Float, Float>>,
    val topInRoot: Int,
    val height: Int,
)

internal val LocalNGridScrollIndicator =
    staticCompositionLocalOf<MutableState<NGridScrollIndicatorLayout?>?> { null }

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
            host.value = NGridScrollIndicatorLayout(
                visibleRange = visibleRange,
                topInRoot = coordinates.positionInRoot().y.roundToInt(),
                height = coordinates.size.height,
            )
        })
    }
}

@Composable
internal fun BoxScope.NGridScrollIndicatorOverlay(
    layout: NGridScrollIndicatorLayout,
    hostTopInRoot: Int,
) {
    val height = with(LocalDensity.current) { layout.height.toDp() }
    Box(
        Modifier.align(Alignment.TopEnd)
            .offset { IntOffset(0, layout.topInRoot - hostTopInRoot) }
            .height(height)
            .width(2.dp)
    ) {
        key(layout.visibleRange) {
            VerticalScrollbar { layout.visibleRange.value }
        }
    }
}
