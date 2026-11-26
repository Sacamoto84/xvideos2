package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.pagination.PageNavigationBar
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.ui.lazyrow123.atom.InfiniteScrollSwitch

@Composable
internal fun PageableFeed(
    host: LazyRow123Host,
    header: @Composable (scrollMode: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onClickOpenProfile: (String) -> Unit = {},
    onAppendLoaded: (LazyPagingItems<GifsInfo>) -> Unit = {},
) {
    val navigation = checkNotNull(host.pageNavigation)
    val state by navigation.state.collectAsStateWithLifecycle()
    val query = state.query
    val pagerState = if (state.infiniteScroll) null else {
        key(query) { rememberPagerState(initialPage = state.pageIndex) { state.pageCount } }
    }
    if (pagerState != null) {
        LaunchedEffect(host, pagerState, query) {
            snapshotFlow { pagerState.settledPage }.collect { index ->
                host.onNumberedPageSettled(query, index)
            }
        }
    }

    val renderHeader: @Composable () -> Unit = {
        header {
            InfiniteScrollSwitch(checked = state.infiniteScroll, onCheckedChange = host::setInfiniteScroll)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Theme.background,
        bottomBar = {
            if (pagerState != null) {
                PageNavigationBar(
                    value = pagerState.currentPage,
                    max = state.pageCount,
                    onChange = { index -> pagerState.requestScrollToPage(index) },
                    modifier = Modifier.background(Theme.tabLevel1),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Theme.background)) {
            if (pagerState == null) {
                LazyRow123(
                    host = host,
                    modifier = Modifier.fillMaxSize(),
                    onClickOpenProfile = onClickOpenProfile,
                    contentBeforeList = renderHeader,
                    onAppendLoaded = onAppendLoaded,
                    fastScrollSessionKey = query,
                )
            } else {
                PagedFeedContent(header = renderHeader) { gridModifier ->
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                        val pageHost = remember(host, query, index) { host.numberedPageHost(query, index) }
                        val columns = host.columns
                        LaunchedEffect(pageHost, columns) { pageHost.columns = columns }
                        LazyRow123(
                            host = pageHost,
                            modifier = Modifier.fillMaxSize().then(gridModifier),
                            onClickOpenProfile = onClickOpenProfile,
                            showScrollButtons = false,
                            onAppendLoaded = onAppendLoaded,
                            enableFastScroll = pagerState.currentPage == index,
                            fastScrollSessionKey = pageHost,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PageableFeedPreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("PageableFeed")
    }
}
