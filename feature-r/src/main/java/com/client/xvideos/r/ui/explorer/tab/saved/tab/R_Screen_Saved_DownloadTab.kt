package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.P2pSendChooserDialog
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.common.share.useCaseShareGifs
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.DownloadListItem
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreen
import com.client.xvideos.ui.theme.XvideosTheme

object R_Screen_Saved_DownloadTab : Screen {

    private fun readResolve(): Any = R_Screen_Saved_DownloadTab

    override val key: ScreenKey = "R_Screen_Saved_DownloadTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenSavedDownloadSM>()
        val context = LocalContext.current

        val downloadRed by vm.downloadRed.downloadList.collectAsStateWithLifecycle()
        val state = rememberLazyListState()

        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
            gridState = state, itemsToIgnore = 0
        )

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

        val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

        SavedDownloadTabContent(
            downloadList = downloadRed,
            state = state,
            topInset = getTopInsetDp(),
            scrollPercentProvider = scrollPercentProvider,
            onItemClick = onItemClickHandler,
            onFullScreenClick = onItemClickHandler,
            onShareClick = onShareClickHandler,
            onDeleteClick = onDeleteClickHandler
        )
    }
}

@Composable
fun SavedDownloadTabContent(
    downloadList: List<GifsInfo>,
    state: LazyListState,
    topInset: Dp,
    scrollPercentProvider: () -> Pair<Float, Float>,
    onItemClick: (GifsInfo) -> Unit,
    onFullScreenClick: (GifsInfo) -> Unit,
    onShareClick: (GifsInfo) -> Unit,
    onDeleteClick: (GifsInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topInset)
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Загрузки",
                    color = Theme.R.colorYellow,
                    fontSize = 18.sp,
                    fontFamily = Theme.R.fontFamilyPopinsRegular
                )
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(downloadList, key = { it.id }, contentType = { "download_item" }) { item ->
                    DownloadListItem(
                        item = item,
                        onItemClick = onItemClick,
                        onFullScreenClick = onFullScreenClick,
                        onShareClick = onShareClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .width(2.dp)
            ) {
                VerticalScrollbar(scrollPercentProvider)
            }
        }
    }
}

@Preview
@Composable
private fun SavedDownloadTabPreview() {
    XvideosTheme(darkTheme = true) {
        SavedDownloadTabContent(
            downloadList = emptyList(),
            state = rememberLazyListState(),
            topInset = 24.dp,
            scrollPercentProvider = { 0f to 1f },
            onItemClick = {},
            onFullScreenClick = {},
            onShareClick = {},
            onDeleteClick = {}
        )
    }
}
