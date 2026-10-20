package com.client.xvideos.l.ui.screens.albumLandingTag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.client.xvideos.common.ui.adaptive.currentWidthClass
import com.client.xvideos.common.ui.adaptive.rowItemWidth
import com.client.xvideos.common.ui.adaptive.rowVisibleItems
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.ui.screens.albumLandingTag.molecule.LandingTagSectionItem
import com.client.xvideos.l.ui.screens.albumLandingTag.molecule.LandingTagTopBar
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.AlbumListPageError

class ScreenLAlbumLandingTag(val tag: String) : Screen {

    override val key: ScreenKey = "ScreenLAlbumLandingTag:$tag"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenLAlbumLandingTagSM, ScreenLAlbumLandingTagSM.Factory> { factory -> factory.create(tag) }
        val albumTopHits by vm.albumTopHits.collectAsStateWithLifecycle()
        val loadError by vm.loadError.collectAsStateWithLifecycle()
        val onRetry: () -> Unit = remember(vm) { { vm.retry() } }
        val items = albumTopHits?.sections
        val title = albumTopHits?.title
        val itemWidth = rowItemWidth()
        val itemsPerRow = rowVisibleItems(currentWidthClass())

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }
        BackHandler(onBack = onBack)
        val onAlbumClick: (Long) -> Unit = remember(navigator) {
            { albumId -> navigator.push(ScreenLAlbum(albumId)) }
        }
        val onSeeAllClick: (Landing_page_albumSection) -> Unit = remember(navigator, vm, title, tag) {
            { item ->
                val filter = vm.createFilter(item)
                navigator.push(
                    L_ScreenAlbumList.create(
                        filter = filter,
                        title = "Tag: ${title ?: tag}"
                    )
                )
            }
        }
        val topBarTitle = remember(title, tag) { "Tag: ${title ?: tag}" }

        ScreenLAlbumLandingTagContent(
            title = topBarTitle,
            sections = items.orEmpty(),
            loadError = loadError,
            state = vm.listPosition.rememberState(),
            itemWidth = itemWidth,
            itemsPerRow = itemsPerRow,
            onBack = onBack,
            onAlbumClick = onAlbumClick,
            onSeeAllClick = onSeeAllClick,
            onRetry = onRetry
        )
    }
}

@Composable
fun ScreenLAlbumLandingTagContent(
    title: String,
    sections: List<Landing_page_albumSection>,
    loadError: String?,
    state: LazyListState,
    itemWidth: Dp,
    itemsPerRow: Int,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: (Landing_page_albumSection) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.background,
        topBar = {
            LandingTagTopBar(
                title = title,
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Theme.background)
        ) {
            if (sections.isEmpty() && loadError != null) {
                AlbumListPageError(message = loadError, onRetry = onRetry)
                return@Box
            }
            LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                // Индекс в ключе: поле приходит с сервера, и повтор или пустое значение уронили бы список.
                itemsIndexed(
                    items = sections,
                    key = { index, item -> "${item.title}#$index" }
                ) { _, item ->
                    LandingTagSectionItem(
                        item = item,
                        itemWidth = itemWidth,
                        itemsPerRow = itemsPerRow,
                        onAlbumClick = onAlbumClick,
                        onSeeAllClick = onSeeAllClick
                    )
                }

                item {
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ScreenLAlbumLandingTagContentPreview() {
    ScreenLAlbumLandingTagContent(
        title = "Tag: Sample",
        sections = emptyList(),
        loadError = null,
        state = rememberLazyListState(),
        itemWidth = 117.dp,
        itemsPerRow = 3,
        onBack = {},
        onAlbumClick = {},
        onSeeAllClick = {},
        onRetry = {}
    )
}
