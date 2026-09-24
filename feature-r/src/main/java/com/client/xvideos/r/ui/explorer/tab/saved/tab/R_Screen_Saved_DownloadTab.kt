package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.activity.compose.BackHandler
import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import com.client.xvideos.common.util.getTopInsetDp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.util.toPrettyCount3
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.share.useCaseShareGifs
import com.client.xvideos.r.model.GifsInfo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.P2pSendChooserDialog
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreen
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import java.io.File
import javax.inject.Inject

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

        // Без `by`: см. VerticalScrollbar — чтение позиции скролла здесь
        // перекомпоновывало бы весь экран на каждом кадре.
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

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = getTopInsetDp())
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        "Загрузки",
                        color = Theme.R.colorYellow,
                        fontSize = 18.sp,
                        fontFamily = Theme.R.fontFamilyPopinsRegular
                    )
                }
            }
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
                    items(downloadRed, key = { it.id }) { item ->
                        DownloadListItem(
                            item = item,
                            onItemClick = onItemClickHandler,
                            onFullScreenClick = onItemClickHandler,
                            onShareClick = onShareClickHandler,
                            onDeleteClick = onDeleteClickHandler
                        )
                    }
                }

                //---- Скролл ----
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
}

@Composable
private fun DownloadListItem(
    item: GifsInfo,
    onItemClick: (GifsInfo) -> Unit,
    onFullScreenClick: (GifsInfo) -> Unit,
    onShareClick: (GifsInfo) -> Unit,
    onDeleteClick: (GifsInfo) -> Unit
) {
    val onClick = remember(item, onItemClick) { { onItemClick(item) } }
    val onFullScreen = remember(item, onFullScreenClick) { { onFullScreenClick(item) } }
    val onShare = remember(item, onShareClick) { { onShareClick(item) } }
    val onDelete = remember(item, onDeleteClick) { { onDeleteClick(item) } }

    val imagePath = remember(item.userName, item.id) {
        AppPath.r_cache_download + "/" + item.userName + "/" + item.id + ".jpg"
    }
    val mp4Path = remember(item.userName, item.id) {
        AppPath.r_cache_download + "/" + item.userName + "/" + item.id + ".mp4"
    }
    val size = remember(mp4Path) { File(mp4Path).length().toPrettyCount3() }
    val autoSize = remember { TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 18.sp) }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp, Theme.R.colorBorderGray,
                RoundedCornerShape(8.dp)
            )
            .background(Theme.tabLevel3)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((72 * 1920f / 1080).toInt().dp)
        ) {
            UrlImage(
                imagePath,
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Name: " + item.userName,
                    color = Color.White,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    fontSize = 18.sp,
                    maxLines = 1
                )

                BasicText(
                    text = "ID: " + item.id,
                    style = TextStyle(
                        color = Color.White,
                        fontFamily = Theme.R.fontFamilyPopinsRegular,
                        fontSize = 18.sp
                    ),
                    autoSize = autoSize,
                    maxLines = 1
                )

                Text(
                    "Size: $size",
                    color = Color.White,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    fontSize = 18.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.End),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onFullScreen) {
                        Icon(
                            Icons.Outlined.Fullscreen,
                            contentDescription = "Открыть во весь экран",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "Поделиться",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Удалить загрузку",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Stable
class ScreenSavedDownloadSM @Inject constructor(
    val downloadRed: DownloadRed
) : ScreenModel {
    fun delete(item: GifsInfo) {
        downloadRed.delete(item)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedDownload {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedDownloadSM::class)
    abstract fun bindScreenRedSavedDownloadScreenModel(hiltListScreenModel: ScreenSavedDownloadSM): ScreenModel
}
