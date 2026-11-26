package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLayoutInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.scroll.MediaFastScroll
import com.client.xvideos.common.ui.scroll.MediaFastScrollDrag
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.safeAspectRatio
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
internal fun LPictureFastScroll(
    gridState: LazyStaggeredGridState,
    pictures: List<PicsDetails>,
    columns: Int,
    firstItemIndex: Int,
    hasItemMenu: Boolean,
    sessionKey: Any?,
    hazeState: HazeState,
    bottomClearance: Dp,
    modifier: Modifier = Modifier,
) {
    val viewport by remember(gridState) { derivedStateOf { gridState.layoutInfo.viewportSize } }
    val density = LocalDensity.current
    var prefixHeight by remember(gridState, viewport.width, firstItemIndex) { mutableIntStateOf(0) }
    LaunchedEffect(gridState, viewport.width, firstItemIndex) {
        snapshotFlow { gridState.layoutInfo.prefixHeight(firstItemIndex) }
            .filterNotNull()
            .collect { prefixHeight = it }
    }

    if (viewport.width <= 0 || viewport.height <= 0) return
    val spacing = with(density) { Theme.Content.itemSpacing.roundToPx() }
    val minimumHeight = with(density) { if (hasItemMenu) 48.dp.roundToPx() else 1 }
    val geometry = remember(pictures, viewport.width, columns, spacing, prefixHeight, firstItemIndex, minimumHeight) {
        val cells = with(StaggeredGridCells.Fixed(columns)) {
            density.calculateCrossAxisCellSizes(viewport.width, spacing)
        }
        calculateLPictureScrollGeometry(
            cellWidths = cells,
            spacing = spacing,
            prefixHeight = prefixHeight,
            firstItemIndex = firstItemIndex,
            minimumItemHeight = minimumHeight,
            itemCount = pictures.size,
        ) { pictures[it].safeAspectRatio() }
    }
    var lastSample by remember(gridState, viewport, columns, spacing, prefixHeight, firstItemIndex, sessionKey) {
        mutableStateOf<ScrollSample?>(null)
    }
    LaunchedEffect(gridState, viewport, columns, spacing, prefixHeight, firstItemIndex, sessionKey) {
        snapshotFlow { gridState.layoutInfo }.collect { layout ->
            val position = gridState.pixelPosition(geometry, layout, lastSample)
            lastSample = captureScrollSample(layout, position)
        }
    }

    MediaFastScroll(
        modifier = modifier,
        hazeState = hazeState,
        sessionKey = sessionKey,
        canScrollBackward = { gridState.canScrollBackward },
        canScrollForward = { gridState.canScrollForward },
        isScrollInProgress = { gridState.isScrollInProgress },
        positionFraction = {
            geometry.fractionAt(gridState, lastSample, viewport.height)
        },
        mediaRange = { firstItemIndex until firstItemIndex + pictures.size },
        beginDrag = { range ->
            lPictureFastScrollDrag(gridState, geometry, viewport.height, range, lastSample)
        },
        stopScroll = { gridState.stopScroll(MutatePriority.UserInput) },
        bottomClearance = bottomClearance,
    )
}

private fun LPictureScrollGeometry.fractionAt(
    gridState: LazyStaggeredGridState,
    lastSample: ScrollSample?,
    viewportHeight: Int,
): Float {
    val maxScroll = maxScroll(viewportHeight)
    return when {
        !gridState.canScrollBackward -> 0f
        !gridState.canScrollForward -> 1f
        maxScroll == 0L -> 0f
        else -> (gridState.pixelPosition(this, gridState.layoutInfo, lastSample).toDouble() / maxScroll).toFloat()
    }
}

private fun lPictureFastScrollDrag(
    gridState: LazyStaggeredGridState,
    frozenGeometry: LPictureScrollGeometry,
    viewportHeight: Int,
    range: IntRange,
    initialSample: ScrollSample?,
): MediaFastScrollDrag {
    val extent = frozenGeometry.maxScroll(viewportHeight)
    val positionAt: (Float) -> Long = { (extent * it.toDouble()).roundToLong() }
    var dragSample = initialSample
    val move: (Float) -> Unit = { fraction ->
        val target = positionAt(fraction)
        val layout = gridState.layoutInfo
        when {
            fraction <= 0f -> if (gridState.canScrollBackward) gridState.requestScrollToItem(0)
            fraction >= 1f && range.last == layout.totalItemsCount - 1 -> {
                if (gridState.canScrollForward) gridState.requestScrollToItem(range.last)
            }
            else -> {
                val position = gridState.pixelPosition(frozenGeometry, layout, dragSample)
                dragSample = captureScrollSample(layout, position)
                scrollFastToTarget(gridState, frozenGeometry, layout, range, target, position)
            }
        }
    }
    return MediaFastScrollDrag(
        moveTo = move,
        positionNumber = { frozenGeometry.numberAt(positionAt(it)) },
    )
}

private data class ScrollSample(
    val layout: LazyStaggeredGridLayoutInfo,
    val anchors: List<ScrollAnchor>,
    val position: Long,
)

private data class ScrollAnchor(val index: Int, val key: Any, val lane: Int, val y: Int, val height: Int)

private fun LazyStaggeredGridLayoutInfo.prefixHeight(firstItemIndex: Int): Int? {
    val header = visibleItemsInfo.firstOrNull { it.index == 0 } ?: return null
    val firstPicture = visibleItemsInfo.firstOrNull { it.index == firstItemIndex }
    return when {
        firstPicture != null -> (firstPicture.offset.y - header.offset.y).coerceAtLeast(0)
        firstItemIndex == 1 -> header.size.height + mainAxisItemSpacing
        else -> null
    }
}

private fun LazyStaggeredGridState.pixelPosition(
    geometry: LPictureScrollGeometry,
    layout: LazyStaggeredGridLayoutInfo,
    previous: ScrollSample?,
): Long {
    val maxScroll = geometry.maxScroll(layout.viewportSize.height)
    if (!canScrollBackward) return 0L
    if (!canScrollForward) return maxScroll
    if (previous?.layout === layout) return previous.position.coerceIn(0L, maxScroll)

    // Match a card present in both layouts. Its measured movement stays continuous across rows.
    if (previous != null) {
        var previousIndex = 0
        for (item in layout.visibleItemsInfo) {
            while (previousIndex < previous.anchors.size && previous.anchors[previousIndex].index < item.index) {
                previousIndex++
            }
            val anchor = previous.anchors.getOrNull(previousIndex) ?: break
            if (anchor.index == item.index && anchor.matches(item)) {
                return (previous.position + anchor.y - item.offset.y).coerceIn(0L, maxScroll)
            }
        }
    }

    val estimate = layout.visibleItemsInfo.firstNotNullOfOrNull { item ->
        geometry.topOf(item.index)?.minus(item.offset.y)
    }
    return (estimate ?: 0L).coerceIn(0L, maxScroll)
}

private fun captureScrollSample(layout: LazyStaggeredGridLayoutInfo, position: Long) = ScrollSample(
    layout = layout,
    anchors = layout.visibleItemsInfo.map { ScrollAnchor(it.index, it.key, it.lane, it.offset.y, it.size.height) },
    position = position,
)

private fun scrollFastToTarget(
    gridState: LazyStaggeredGridState,
    geometry: LPictureScrollGeometry,
    layout: LazyStaggeredGridLayoutInfo,
    range: IntRange,
    target: Long,
    position: Long,
) {
    val delta = target - position
    if (delta == 0L) return
    val anchor = layout.visibleItemsInfo.firstOrNull { it.index in range }
    if (anchor != null && abs(delta) <= layout.viewportSize.height.toLong() * 2) {
        // Compose shifts all active lanes together when the requested card is visible.
        gridState.requestScrollToItem(anchor.index, delta.toInt() - anchor.offset.y)
    } else {
        val destination = geometry.targetAt(target)
        gridState.requestScrollToItem(destination.index, destination.offset)
    }
}

private fun ScrollAnchor.matches(item: LazyStaggeredGridItemInfo): Boolean =
    key == item.key && lane == item.lane && height == item.size.height

@Preview
@Composable
private fun LPictureFastScrollPreview() {
    LPictureFastScroll(
        gridState = rememberLazyStaggeredGridState(),
        pictures = emptyList(),
        columns = 2,
        firstItemIndex = 1,
        hasItemMenu = false,
        sessionKey = Unit,
        hazeState = HazeState(),
        bottomClearance = 0.dp,
    )
}
