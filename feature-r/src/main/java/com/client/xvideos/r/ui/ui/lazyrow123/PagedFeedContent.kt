package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Одна шапка над пейджером. Вертикальная сетка двигает её в пределах своего скролла. */
@Composable
internal fun PagedFeedContent(
    header: @Composable () -> Unit,
    pages: @Composable (gridModifier: Modifier) -> Unit,
) {
    var headerHeight by remember { mutableIntStateOf(0) }
    var collapsedFraction by rememberSaveable { mutableFloatStateOf(0f) }
    val headerScroll = rememberScrollableState { delta ->
        if (headerHeight == 0) return@rememberScrollableState 0f
        val previous = collapsedFraction * headerHeight
        val next = (previous - delta).coerceIn(0f, headerHeight.toFloat())
        collapsedFraction = next / headerHeight
        previous - next
    }
    val gridScrollConnection = remember(headerScroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (available.y < 0f) Offset(0f, headerScroll.dispatchRawDelta(available.y)) else Offset.Zero

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                if (available.y > 0f) Offset(0f, headerScroll.dispatchRawDelta(available.y)) else Offset.Zero
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .layout { measurable, constraints ->
                    val headerContent = measurable.measure(
                        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                    )
                    val offset = (headerContent.height * collapsedFraction).roundToInt()
                    val gridSpace = minOf(96.dp.roundToPx(), constraints.maxHeight / 2)
                    val visibleHeight = minOf(headerContent.height - offset, constraints.maxHeight - gridSpace)
                    layout(headerContent.width, visibleHeight) { headerContent.placeRelative(0, -offset) }
                }
                .onSizeChanged { headerHeight = it.height }
                .scrollable(state = headerScroll, orientation = Orientation.Vertical)
        ) {
            header()
        }
        Box(Modifier.weight(1f)) {
            // Связь внутри PullToRefresh: жест вниз сначала раскрывает шапку у начала сетки.
            pages(Modifier.nestedScroll(gridScrollConnection))
        }
    }
}
