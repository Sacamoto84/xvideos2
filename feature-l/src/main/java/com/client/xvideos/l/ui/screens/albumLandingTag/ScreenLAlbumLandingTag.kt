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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
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
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.ui.screens.albumLandingTag.molecule.LandingTagSectionItem
import com.client.xvideos.l.ui.screens.albumLandingTag.molecule.LandingTagTopBar
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList

class ScreenLAlbumLandingTag(val tag: String) : Screen {

    override val key: ScreenKey = "ScreenLAlbumLandingTag:$tag"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenLAlbumLandingTagSM, ScreenLAlbumLandingTagSM.Factory> { factory -> factory.create(tag) }
        val albumTopHits by vm.albumTopHits.collectAsStateWithLifecycle()
        val items = albumTopHits?.sections
        val title = albumTopHits?.title
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp

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
            state = vm.state,
            screenWidth = screenWidth,
            onBack = onBack,
            onAlbumClick = onAlbumClick,
            onSeeAllClick = onSeeAllClick
        )
    }
}

@Composable
fun ScreenLAlbumLandingTagContent(
    title: String,
    sections: List<Landing_page_albumSection>,
    state: LazyListState,
    screenWidth: Dp,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: (Landing_page_albumSection) -> Unit,
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
            LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                items(
                    items = sections,
                    key = { it.title }
                ) { item ->
                    LandingTagSectionItem(
                        item = item,
                        screenWidth = screenWidth,
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
        state = rememberLazyListState(),
        screenWidth = 360.dp,
        onBack = {},
        onAlbumClick = {},
        onSeeAllClick = {}
    )
}
