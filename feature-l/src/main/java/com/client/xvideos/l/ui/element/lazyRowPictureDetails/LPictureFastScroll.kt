package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLayoutInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.scroll.MediaFastScroll
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
    val currentGeometry by rememberUpdatedState(geometry)
    var lastSample by remember(gridState, viewport, columns, spacing, prefixHeight, firstItemIndex, sessionKey) {
        mutableStateOf<ScrollSample?>(null)
    }
    LaunchedEffect(gridState, viewport, columns, spacing, prefixHeight, firstItemIndex, sessionKey) {
        snapshotFlow { gridState.layoutInfo }.collect { layout ->
            val position = gridState.pixelPosition(currentGeometry, layout, lastSample)
            lastSample = ScrollSample(
                layout,
                layout.visibleItemsInfo.map { ScrollAnchor(it.index, it.key, it.lane, it.offset.y, it.size.height) },
                position,
            )
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
            val maxScroll = geometry.maxScroll(viewport.height)
            when {
                !gridState.canScrollBackward -> 0f
                !gridState.canScrollForward -> 1f
                maxScroll == 0L -> 0f
                else -> (gridState.pixelPosition(geometry, gridState.layoutInfo, lastSample).toDouble() / maxScroll).toFloat()
            }
        },
        mediaRange = { firstItemIndex until firstItemIndex + pictures.size },
        topVisibleMediaNumber = { range ->
            val layout = gridState.layoutInfo
            pictureNumberAtViewportStart(
                pictures = layout.visibleItemsInfo.asSequence().map {
                    VisiblePicture(index = it.index, top = it.offset.y, height = it.size.height)
                },
                mediaRange = range,
                viewportStart = layout.viewportStartOffset,
                viewportEnd = layout.viewportEndOffset,
            )
        },
        beginDrag = { range ->
            val frozenGeometry = geometry
            val extent = frozenGeometry.maxScroll(viewport.height)
            val move: (Float) -> Unit = { fraction ->
                val target = (extent * fraction.toDouble()).roundToLong()
                val layout = gridState.layoutInfo
                when {
                    fraction <= 0f -> if (gridState.canScrollBackward) gridState.requestScrollToItem(0)
                    fraction >= 1f && range.last == layout.totalItemsCount - 1 -> {
                        if (gridState.canScrollForward) gridState.requestScrollToItem(range.last)
                    }
                    else -> {
                        val delta = target - gridState.pixelPosition(currentGeometry, layout, lastSample)
                        if (delta != 0L) {
                            val anchor = layout.visibleItemsInfo.firstOrNull { it.index in range }
                            if (anchor != null && abs(delta) <= viewport.height.toLong() * 2) {
                                // Compose shifts all active lanes together when the requested card is visible.
                                gridState.requestScrollToItem(anchor.index, delta.toInt() - anchor.offset.y)
                            } else {
                                val destination = frozenGeometry.targetAt(target)
                                gridState.requestScrollToItem(destination.index, destination.offset)
                            }
                        }
                    }
                }
            }
            move
        },
        stopScroll = { gridState.stopScroll(MutatePriority.UserInput) },
        bottomClearance = bottomClearance,
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

private fun ScrollAnchor.matches(item: LazyStaggeredGridItemInfo): Boolean =
    key == item.key && lane == item.lane && height == item.size.height

internal data class VisiblePicture(val index: Int, val top: Int, val height: Int)

internal fun pictureNumberAtViewportStart(
    pictures: Sequence<VisiblePicture>,
    mediaRange: IntRange,
    viewportStart: Int,
    viewportEnd: Int,
): Int? = pictures
    .filter { it.index in mediaRange && it.top < viewportEnd && it.top + it.height > viewportStart }
    // Compare the real tops so a card crossing the edge cannot mask a closer one.
    .minWithOrNull(compareBy({ abs(it.top - viewportStart) }, { it.index }))
    ?.let { it.index - mediaRange.first + 1 }
