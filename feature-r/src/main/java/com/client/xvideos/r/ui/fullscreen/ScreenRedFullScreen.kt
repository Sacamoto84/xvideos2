package com.client.xvideos.r.ui.fullscreen

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.ScreenTransition
import com.client.xvideos.common.ui.statusbar.HideStatusBarEffect
import com.client.xvideos.r.common.downloader.DownloadSort
import com.client.xvideos.r.common.downloader.sortedItems
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.fullscreen.molecule.RedFullScreenFeed
import com.client.xvideos.r.ui.fullscreen.molecule.RedFullScreenPageOverlay
import com.client.xvideos.r.ui.fullscreen.molecule.RedFullScreenSingle
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.RFeedSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalVoyagerApi::class)
internal class ScreenRedFullScreen(
    val item: GifsInfo,
    private val feedKey: String? = null,
    private val startIndex: Int = 0,
    private val downloadSort: DownloadSort? = null,
    @Transient private val downloadItems: List<GifsInfo>? = null,
) : Screen, ScreenTransition {

    override val key: ScreenKey = "RedFullScreen:${item.id}:$startIndex"

    @Composable
    override fun Content() {
        HideStatusBarEffect()
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenRedFullScreenSM>()
        val host = remember(feedKey) { feedKey?.let { RFeedSessionStore.get(it) } }
        var index = startIndex
        var items = downloadItems
        // При восстановлении процесса перечитываем список с диска в исходном порядке сортировки.
        val sort = downloadSort
        if (items == null && sort != null) {
            val downloads by vm.downloadRed.downloads.collectAsStateWithLifecycle()
            val sortedItems = remember(downloads, sort) { downloads.sortedItems(sort) }
            index = remember(sortedItems, item) {
                sortedItems.indexOfFirst { it.id == item.id && it.userName == item.userName }
            }
            if (index >= 0) items = sortedItems
        }
        val downloadPager = remember(items) { items?.let { flowOf(PagingData.from(it)) } }

        ScreenRedFullScreenContent(
            host = host,
            startIndex = index,
            item = item,
            vm = vm,
            navigator = navigator,
            downloadPager = downloadPager,
        )
    }

    override fun enter(lastEvent: StackEvent): EnterTransition {
        return fadeIn(tween(300))
    }

    override fun exit(lastEvent: StackEvent): ExitTransition {
        return fadeOut(tween(300))
    }
}

@Composable
fun ScreenRedFullScreenContent(
    host: LazyRow123Host?,
    startIndex: Int,
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    downloadPager: Flow<PagingData<GifsInfo>>? = null,
) {
    val pageOverlay: @Composable (GifsInfo, () -> Unit) -> Unit = { overlayItem, onBack ->
        RedFullScreenPageOverlay(
            item = overlayItem,
            player = vm.player,
            savedRed = vm.savedRed,
            downloadRed = vm.downloadRed,
            block = vm.block,
            redApi = vm.redApi,
            search = vm.search,
            navigator = navigator,
            onBack = onBack,
            accountLikes = host?.isAccountLikes == true,
            accountCollectionId = host?.accountCollectionId,
        )
    }

    Box(modifier = modifier) {
        val pager = host?.pager ?: downloadPager
        if (pager != null) {
            RedFullScreenFeed(
                pager = pager,
                startIndex = startIndex,
                fallbackItem = item,
                player = vm.player,
                downloadRed = vm.downloadRed,
                navigator = navigator,
                pageOverlay = pageOverlay,
                onPageChanged = { page ->
                    host?.let {
                        it.currentIndex = page
                        it.returnToIndex = page
                    }
                },
            )
        } else {
            RedFullScreenSingle(
                item = item,
                player = vm.player,
                downloadRed = vm.downloadRed,
                navigator = navigator,
                pageOverlay = pageOverlay
            )
        }
    }
}

@Preview
@Composable
private fun ScreenRedFullScreenPreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("ScreenRedFullScreen", color = Color.White)
    }
}
