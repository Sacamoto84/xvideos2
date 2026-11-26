package com.client.xvideos.r.ui.explorer.tab.niches.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveListPadding
import com.client.xvideos.common.ui.atom.FloatingScrollButtons
import com.client.xvideos.common.ui.scroll.LazyListFastScroll
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.explorer.tab.niches.atom.RefreshMini
import com.client.xvideos.r.ui.explorer.tab.niches.model.NichesCacheState
import com.client.xvideos.r.ui.explorer.tab.niches.model.NichesTabActions
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@Composable
internal fun NichesTabBody(
    listState: LazyListState,
    niches: List<Niche>,
    sortType: Order,
    searchQuery: String,
    isSearchFocused: Boolean,
    savedRed: () -> SavedRed?,
    cacheState: NichesCacheState,
    actions: NichesTabActions,
    modifier: Modifier = Modifier,
) {
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyColumn(listState, itemsToIgnore = 1)
    val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val hazeState = remember { HazeState() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = adaptiveListPadding(PaddingValues(top = getTopInsetDp()))
        ) {
            item(key = "refresh_mini", contentType = "refresh_mini") {
                AnimatedVisibility(cacheState.lastModifiedHour > 72, enter = fadeIn(), exit = fadeOut()) {
                    RefreshMini(
                        onRefreshNichesCacheClick = actions.onRefreshNichesCacheClick,
                        nichesCacheProgress = cacheState.progress,
                        cacheHour = cacheState.lastModifiedHour
                    )
                }
            }

            if (niches.isEmpty()) {
                item(key = "empty_placeholder", contentType = "empty_placeholder") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No results", color = Color.Gray, fontFamily = Theme.R.fontFamilyDMsanss)
                    }
                }
            } else {
                val currentRed = savedRed()
                items(items = niches, key = { it.id }, contentType = { "niche" }) { item ->
                    if (currentRed != null) {
                        NicheItemRow(
                            item = item,
                            savedRed = currentRed,
                            onNicheClick = actions.onNicheClick,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp, horizontal = 8.dp)
                                .fillMaxWidth()
                                .height(78.dp)
                                .background(Theme.tabLevel3, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                modifier = Modifier.padding(start = 16.dp),
                                fontFamily = Theme.R.fontFamilyDMsanss
                            )
                        }
                    }
                }
            }
        }

        LazyListFastScroll(
            hazeState = hazeState,
            state = listState,
            modifier = Modifier.matchParentSize(),
            itemCount = { niches.size },
            visibleRange = scrollPercentProvider,
            firstItemIndex = 1,
            sessionKey = sortType to searchQuery,
            bottomClearance = if (isSearchFocused) {
                0.dp
            } else {
                Theme.ScrollFab.size * 2 + Theme.ScrollFab.spacing + 16.dp
            },
        )

        FloatingScrollButtons(
            visible = niches.isNotEmpty() && !isSearchFocused,
            showScrollToTop = listState.canScrollBackward,
            showScrollToBottom = listState.canScrollForward,
            hazeState = hazeState,
            contentColor = Theme.ScrollFab.contentColorR,
            onScrollToTop = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                actions.onUpClick()
            },
            onScrollToBottom = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch {
                    val lastIndex = listState.layoutInfo.totalItemsCount - 1
                    if (lastIndex >= 0) listState.scrollToItem(lastIndex)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun NichesTabBodyPreview() {
    NichesTabBody(
        listState = rememberLazyListState(),
        niches = listOf(Niche("1", "Sample", 10, 100, "", null)),
        sortType = Order.NICHES_SUBSCRIBERS_D,
        searchQuery = "",
        isSearchFocused = false,
        savedRed = { null },
        cacheState = NichesCacheState(1, 1f, 1L),
        actions = NichesTabActions({}, {}, {}, {}),
    )
}
