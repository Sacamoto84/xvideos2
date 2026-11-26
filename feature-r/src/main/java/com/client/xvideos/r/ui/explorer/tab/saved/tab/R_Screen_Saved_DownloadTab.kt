package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.P2pSendChooserDialog
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.ui.scroll.LazyGridFastScroll
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.common.downloader.DownloadSort
import com.client.xvideos.r.common.downloader.RedDownloadsScan
import com.client.xvideos.r.common.downloader.downloadedVideoKey
import com.client.xvideos.r.common.downloader.sortedItems
import com.client.xvideos.r.common.share.useCaseShareGifs
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.model.DownloadView
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.DownloadListItem
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.DownloadSortMenu
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.DownloadViewMenu
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreen
import com.client.xvideos.ui.theme.XvideosTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

object R_Screen_Saved_DownloadTab : Screen {

    private fun readResolve(): Any = R_Screen_Saved_DownloadTab

    override val key: ScreenKey = "R_Screen_Saved_DownloadTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenSavedDownloadSM>()
        val context = LocalContext.current

        val downloads by vm.downloadRed.downloads.collectAsStateWithLifecycle()
        val savedView by Settings.r_download_view.field.collectAsStateWithLifecycle()
        val view = DownloadView.entries.firstOrNull { it.name == savedView } ?: DownloadView.DETAILS
        val savedSort by Settings.r_download_sort.field.collectAsStateWithLifecycle()
        val sort = DownloadSort.entries.firstOrNull { it.name == savedSort } ?: DownloadSort.DATE

        val onItemClickHandler = remember(navigator) {
            { item: GifsInfo -> navigator.push(ScreenRedFullScreen(item)) }
        }

        var chooserItem by remember { mutableStateOf<GifsInfo?>(null) }
        val onDismissChooser = remember { { chooserItem = null } }

        BackHandler(enabled = chooserItem != null, onBack = onDismissChooser)

        val onShareClickHandler = remember {
            { item: GifsInfo -> chooserItem = item }
        }

        chooserItem?.let { item ->
            val onSystemShare = remember(context, item) {
                { useCaseShareGifs(context, item) }
            }
            val onP2pShare = remember(navigator, vm, item) {
                {
                    vm.downloadRed.shareMetaByP2p(item) { bundle ->
                        navigator.push(ScreenP2pSend(P2pSendSource.Ready(bundle)))
                    }
                }
            }
            P2pSendChooserDialog(
                onSystem = onSystemShare,
                onP2p = onP2pShare,
                onDismiss = onDismissChooser,
            )
        }

        val onDeleteClickHandler = remember(vm) {
            { item: GifsInfo -> vm.delete(item) }
        }

        SavedDownloadTabContent(
            downloads = downloads,
            view = view,
            onViewChange = { Settings.r_download_view.setValue(it.name) },
            sort = sort,
            onSortChange = { Settings.r_download_sort.setValue(it.name) },
            state = vm.position.rememberState(),
            topInset = getTopInsetDp(),
            onItemClick = onItemClickHandler,
            onShareClick = onShareClickHandler,
            onDeleteClick = onDeleteClickHandler
        )
    }
}

@Composable
private fun SavedDownloadTabContent(
    downloads: RedDownloadsScan,
    view: DownloadView,
    onViewChange: (DownloadView) -> Unit,
    sort: DownloadSort,
    onSortChange: (DownloadSort) -> Unit,
    state: LazyGridState,
    topInset: Dp,
    onItemClick: (GifsInfo) -> Unit,
    onShareClick: (GifsInfo) -> Unit,
    onDeleteClick: (GifsInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = remember { HazeState() }
    val items = remember(downloads, sort) { downloads.sortedItems(sort) }
    val showGrid = view == DownloadView.GRID
    val columns = if (showGrid) adaptiveUserGridColumns(3) else 1
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(state, numberOfColumns = columns)
    val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SavedTabHeader(
                title = "Загрузки",
                topCutout = topInset,
                trailing = {
                    DownloadSortMenu(sort) { selected ->
                        if (selected != sort) {
                            onSortChange(selected)
                            state.requestScrollToItem(0)
                        }
                    }
                    DownloadViewMenu(view, onViewChange)
                },
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                verticalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
                horizontalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
                state = state,
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            ) {
                items(
                    items = items,
                    key = { downloadedVideoKey(it.userName, it.id) },
                    contentType = { view },
                ) { item ->
                    DownloadListItem(
                        item = item,
                        view = view,
                        videoSize = downloads.videoSize(item),
                        onItemClick = onItemClick,
                        onShareClick = onShareClick,
                        onDeleteClick = onDeleteClick,
                        modifier = if (showGrid) Modifier else Modifier.adaptiveContentWidth(),
                    )
                }
            }

            LazyGridFastScroll(
                hazeState = hazeState,
                state = state,
                modifier = Modifier.matchParentSize(),
                itemCount = { items.size },
                visibleRange = scrollPercentProvider,
                sessionKey = sort to view,
            )
        }
    }
}

@Preview
@Composable
private fun SavedDownloadTabPreview() {
    XvideosTheme(darkTheme = true) {
        val items = List(8) { GifsInfo(id = "sample_${it + 1}", userName = "author") }
        var view by remember { mutableStateOf(DownloadView.DETAILS) }
        var sort by remember { mutableStateOf(DownloadSort.DATE) }
        SavedDownloadTabContent(
            downloads = RedDownloadsScan(items, items.mapIndexed { index, item ->
                downloadedVideoKey(item.userName, item.id) to (index + 1) * 1_500_000L
            }.toMap()),
            view = view,
            onViewChange = { view = it },
            sort = sort,
            onSortChange = { sort = it },
            state = rememberLazyGridState(),
            topInset = 24.dp,
            onItemClick = {},
            onShareClick = {},
            onDeleteClick = {}
        )
    }
}
