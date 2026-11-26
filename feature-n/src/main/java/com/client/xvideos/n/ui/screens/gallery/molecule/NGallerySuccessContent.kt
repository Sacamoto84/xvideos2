package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.common.ui.atom.FloatingScrollButtons
import com.client.xvideos.common.ui.scroll.LazyGridFastScroll
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.ui.screens.gallery.NServerFavoriteState
import com.client.xvideos.n.ui.screens.gallery.model.NGallerySuccessActions
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
internal fun NGallerySuccessContent(
    galleryId: Long,
    gallery: NGallery,
    columns: Int,
    gridState: LazyGridState,
    isFavorite: Boolean,
    canToggleFavorite: Boolean,
    serverFavorite: NServerFavoriteState,
    actions: NGallerySuccessActions,
    pageMenuContent: (@Composable (NPage, Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val hazeState = remember { HazeState() }
    val haptic = LocalHapticFeedback.current
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(
        gridState,
        itemsToIgnore = 1,
        numberOfColumns = adaptiveUserGridColumns(columns),
    )

    Box(modifier = modifier.fillMaxSize()) {
        NGalleryPagesGrid(
            pages = gallery.pages,
            mediaId = gallery.mediaId,
            columns = columns,
            onPageClick = { pageIndex ->
                gallery.pages.getOrNull(pageIndex)?.let { actions.onRead(it.pageNumber) }
            },
            gridState = gridState,
            menuContent = pageMenuContent,
            header = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NGalleryHeader(
                        gallery = gallery,
                        isFavorite = isFavorite,
                        canToggleFavorite = canToggleFavorite,
                        onFavoriteClick = actions.onFavoriteClick,
                        serverFavorite = serverFavorite,
                        onServerFavoriteClick = actions.onServerFavoriteClick,
                        onTagClick = actions.onTagClick
                    )
                    NGalleryRelatedStrip(
                        relatedGalleries = gallery.relatedGalleries,
                        onGalleryClick = actions.onGalleryClick
                    )
                }
            },
            modifier = Modifier.fillMaxSize().hazeSource(hazeState)
        )
        LazyGridFastScroll(
            state = gridState,
            modifier = Modifier.matchParentSize(),
            hazeState = hazeState,
            itemCount = { gallery.pages.size },
            visibleRange = { scrollPercent.value },
            firstItemIndex = 1,
            sessionKey = galleryId,
            bottomClearance = Theme.ScrollFab.size * 2 + Theme.ScrollFab.spacing + 16.dp,
        )
        FloatingScrollButtons(
            visible = gallery.pages.isNotEmpty(),
            showScrollToTop = gridState.canScrollBackward,
            showScrollToBottom = gridState.canScrollForward,
            hazeState = hazeState,
            onScrollToTop = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                gridState.requestScrollToItem(0)
            },
            onScrollToBottom = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                gridState.requestScrollToItem(gallery.pages.size)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun NGallerySuccessContentPreview() {
    NGallerySuccessContent(
        galleryId = 1L,
        gallery = NGallery(id = 1L, mediaId = 1L, title = "Sample Gallery"),
        columns = 2,
        gridState = rememberLazyGridState(),
        isFavorite = false,
        canToggleFavorite = true,
        serverFavorite = NServerFavoriteState(),
        actions = NGallerySuccessActions({}, {}, {}, {}, {}),
        pageMenuContent = null,
    )
}
