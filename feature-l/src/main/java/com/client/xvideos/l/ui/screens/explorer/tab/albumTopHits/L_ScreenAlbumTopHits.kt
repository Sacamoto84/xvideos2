package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumListTopHits
import com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.molecule.TopHitsSectionItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.AlbumListPageError

object L_ScreenAlbumTopHits : Screen {

    override val key: ScreenKey = "L_ScreenAlbumTopHits"

    private fun readResolve(): Any = L_ScreenAlbumTopHits

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLAlbumTopHitsSM = getScreenModel()
        val albumTopHits by vm.albumTopHits.collectAsStateWithLifecycle()
        val loadError by vm.loadError.collectAsStateWithLifecycle()
        val items = albumTopHits?.items
        val onRetry: () -> Unit = remember(vm) { { vm.retry() } }
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val itemWidth = remember(screenWidth) { (screenWidth - 8.dp) / 3 }

        val onAlbumClick: (Long) -> Unit = remember(navigator) {
            { albumId -> navigator.push(ScreenLAlbum(albumId)) }
        }
        val onSeeAllClick: (String, String) -> Unit = remember(navigator) {
            { url, title ->
                navigator.push(
                    L_ScreenAlbumList.create(
                        filter = albumListFilterFromTopHitsUrl(url),
                        title = title
                    )
                )
            }
        }

        L_ScreenAlbumTopHitsContent(
            items = items.orEmpty(),
            loadError = loadError,
            state = vm.state,
            itemWidth = itemWidth,
            onAlbumClick = onAlbumClick,
            onSeeAllClick = onSeeAllClick,
            onRetry = onRetry
        )
    }
}

@Composable
fun L_ScreenAlbumTopHitsContent(
    items: List<AlbumListTopHits>,
    loadError: String?,
    state: LazyListState,
    itemWidth: Dp,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: (String, String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        if (items.isEmpty() && loadError != null) {
            AlbumListPageError(message = loadError, onRetry = onRetry)
            return@Box
        }
        LazyColumn(state = state) {
            items(
                items = items,
                key = { it.title },
                contentType = { "top_hits_section" }
            ) { item ->
                TopHitsSectionItem(
                    item = item,
                    itemWidth = itemWidth,
                    onAlbumClick = onAlbumClick,
                    onSeeAllClick = onSeeAllClick
                )
            }
        }
    }
}

@Preview
@Composable
private fun L_ScreenAlbumTopHitsContentPreview() {
    L_ScreenAlbumTopHitsContent(
        items = emptyList(),
        loadError = null,
        state = rememberLazyListState(),
        itemWidth = 120.dp,
        onAlbumClick = {},
        onSeeAllClick = { _, _ -> },
        onRetry = {}
    )
}
