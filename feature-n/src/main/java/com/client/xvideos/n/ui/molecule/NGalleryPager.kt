package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.TopLoadingBar
import com.client.xvideos.common.ui.scroll.AlbumListPager
import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.atom.NGridScrollIndicator
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryPages
import kotlinx.coroutines.launch

@Composable
fun NGalleryPager(
    pages: NGalleryPages,
    columns: Int,
    onGalleryClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Нет работ",
    itemMenuActions: (@Composable (NGalleryPreview, dismiss: () -> Unit) -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
    hasActiveFilter: Boolean = false,
    allowChallenge: Boolean = true,
    gridTopInset: Dp = 0.dp,
    showVerticalScrollbar: Boolean = false,
    leadingContent: @Composable () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val pager = pages.position.rememberState { pages.totalPages }
    var dismissedChallenge by remember { mutableStateOf<CloudflareBlockedException?>(null) }
    val visibleChallenge = pages.visibleChallenge

    Column(modifier.fillMaxSize().background(Theme.background)) {
        Box(Modifier.weight(1f)) {
            key(pager) {
                AlbumListPager(pager, pages::load) { index ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (val state = pages[index]) {
                            null, NGalleryListUiState.Loading -> CircularProgressIndicator()
                            is NGalleryListUiState.Error -> Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(state.message, color = Color.White)
                                TextButton(onClick = { pages.retry(index) }) { Text("Повторить") }
                            }
                            is NGalleryListUiState.CloudflareRequired ->
                                NCloudflareBanner(onVerifyClick = { dismissedChallenge = null })
                            is NGalleryListUiState.Success -> if (state.items.isEmpty()) {
                                Text(emptyMessage, color = Color.LightGray)
                            } else {
                                val gridState = pages.grid(index).rememberState()
                                NGalleryGrid(
                                    items = state.items,
                                    columns = columns,
                                    onItemClick = onGalleryClick,
                                    itemMenuActions = itemMenuActions,
                                    gridState = gridState,
                                    modifier = Modifier.fillMaxSize(),
                                    topInset = gridTopInset
                                )
                                if (showVerticalScrollbar && index == pager.currentPage) {
                                    NGridScrollIndicator(gridState, columns)
                                }
                            }
                        }
                    }
                }
            }
            if (pages.loading) TopLoadingBar()
        }
        NAlbumListBottomBar(
            currentPage = pager.currentPage + 1,
            totalPages = pages.totalPages,
            onPageChange = { number -> scope.launch { pager.scrollToPage(number - 1) } },
            onFilterClick = onFilterClick,
            hasActiveFilter = hasActiveFilter,
            leadingContent = leadingContent
        )
    }

    if (allowChallenge && visibleChallenge != null && visibleChallenge !== dismissedChallenge) {
        NRequestChallengeDialog(
            challenge = visibleChallenge,
            onDismiss = { dismissedChallenge = visibleChallenge },
            onVerified = { pages.refresh() }
        )
    }
}

@Preview
@Composable
private fun NGalleryPagerPreview() {
    val scope = rememberCoroutineScope()
    val pages = remember(scope) {
        NGalleryPages(scope) { number ->
            val items = (1..30).map { item ->
                NGalleryPreview(id = number * 100L + item, title = "Альбом $number.$item")
            }
            Result.success(NGalleryPageResult(items, page = number, totalPages = 12))
        }
    }
    NGalleryPager(pages, columns = 2, onGalleryClick = {})
}
