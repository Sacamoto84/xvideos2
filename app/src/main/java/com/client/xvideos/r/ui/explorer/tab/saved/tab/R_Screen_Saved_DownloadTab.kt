package com.client.xvideos.r.ui.explorer.tab.saved.tab

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
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
import cafe.adriel.voyager.core.screen.uniqueScreenKey
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
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreen
import com.client.xvideos.r.ui.profile.atom.VerticalScrollbar
import com.client.xvideos.r.ui.profile.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import java.io.File
import javax.inject.Inject

object R_Screen_Saved_DownloadTab : Screen {

    private fun readResolve(): Any = R_Screen_Saved_DownloadTab

    override val key: ScreenKey = uniqueScreenKey

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenSavedDownloadSM>()
        val context = LocalContext.current

        val downloadRed by vm.downloadRed.downloadList.collectAsState()
        val state = rememberLazyListState()

        val scrollPercent by rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
            gridState = state, itemsToIgnore = 0
        )

        val onItemClickHandler = remember(navigator) {
            { item: GifsInfo -> navigator.push(ScreenRedFullScreen(item)) }
        }

        val onShareClickHandler = remember(context) {
            { item: GifsInfo -> useCaseShareGifs(context, item) }
        }

        val onDeleteClickHandler = remember(vm) {
            { item: GifsInfo -> vm.delete(item) }
        }

        Scaffold(topBar = {
            Text(
                ">Загрузки",
                modifier = Modifier.padding(start = 8.dp),
                color = Theme.R.colorYellow,
                fontSize = 18.sp,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )
        }) { padding ->
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
                            onItemClick = { onItemClickHandler(item) },
                            onFullScreenClick = { onItemClickHandler(item) },
                            onShareClick = { onShareClickHandler(item) },
                            onDeleteClick = { onDeleteClickHandler(item) }
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
                    VerticalScrollbar(scrollPercent)
                }
            }
        }
    }
}

@Composable
private fun DownloadListItem(
    item: GifsInfo,
    onItemClick: () -> Unit,
    onFullScreenClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            .clickable(onClick = onItemClick)
    ) {
        val imagePath = AppPath.r_cache_download + "/" + item.userName + "/" + item.id + ".jpg"
        val mp4Path = AppPath.r_cache_download + "/" + item.userName + "/" + item.id + ".mp4"

        val size = remember(mp4Path) { File(mp4Path).length().toPrettyCount3() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((72 * 1920f / 1080).toInt().dp)
        ) {
            UrlImage(
                imagePath,
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .clickable(onClick = onItemClick),
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
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 18.sp),
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
                    IconButton(onClick = onFullScreenClick) {
                        Icon(
                            Icons.Outlined.Fullscreen,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

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
