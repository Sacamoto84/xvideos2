package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.expand_menu_video.ExpandMenuVideo
import com.client.xvideos.r.common.expand_menu_video.ExpandMenuVideoTags
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.video.player_row_mini.RedUrlVideoImageAndLongClick
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.top_this_week.ProfileInfo1

@Composable
fun LazyRow123GridItem(
    item: GifsInfo,
    index: Int,
    host: LazyRow123Host,
    isConnected: Boolean,
    isDownloaded: Boolean,
    isRunLike: Boolean,
    onItemClick: () -> Unit,
    onRefresh: () -> Unit,
    onClickOpenProfile: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    LazyRow123GridItemContent(
        item = item,
        index = index,
        isConnected = isConnected,
        isDownloaded = isDownloaded,
        isRunLike = isRunLike,
        isCollection = host.isCollection,
        visibleProfileInfo = host.visibleProfileInfo,
        columns = host.columns,
        block = { host.block },
        redApi = { host.redApi },
        savedRed = { host.savedRed },
        downloadRed = { host.downloadRed },
        onItemClick = onItemClick,
        onRefresh = onRefresh,
        onClickOpenProfile = onClickOpenProfile,
        onTagClick = onTagClick
    )
}

@Composable
fun LazyRow123GridItemContent(
    item: GifsInfo,
    index: Int,
    isConnected: Boolean,
    isDownloaded: Boolean,
    isRunLike: Boolean,
    isCollection: Boolean,
    visibleProfileInfo: Boolean,
    columns: Int,
    block: () -> BlockRed,
    redApi: () -> RedApi,
    savedRed: () -> SavedRed,
    downloadRed: () -> DownloadRed,
    onItemClick: () -> Unit,
    onRefresh: () -> Unit,
    onClickOpenProfile: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    var isVideo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.padding(1.dp).fillMaxSize().border(1.dp, Color(0xFF555555), RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    )
    {
        RedUrlVideoImageAndLongClick(
            item = item,
            index = index,
            onLongClick = onItemClick,
            onVideo = { isVideo = it },
            isVisibleView = false,
            isVisibleDuration = false,
            play = false,
            isNetConnected = isConnected,
            onFullScreen = onItemClick,
            downloadRed = downloadRed,
        )

        Column(modifier = Modifier.align(Alignment.TopEnd)) {
            ExpandMenuVideo(
                item = item,
                onRunLike = { if (isRunLike) onRefresh() },
                onRefresh = onRefresh,
                isCollection = isCollection,
                block = block,
                redApi = redApi,
                savedRed = savedRed,
                downloadRed = downloadRed
            )

            if (item.tags.isNotEmpty()) { ExpandMenuVideoTags(item = item, onClick = onTagClick) }
        }

        AnimatedVisibility(
            visible = !isVideo,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (visibleProfileInfo) {
                    ProfileInfo1(
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp).align(Alignment.BottomStart),
                        onClick = { onClickOpenProfile(item.userName) },
                        videoItem = item,
                        listUsers = UsersRed.listAllUsers,
                        visibleUserName = columns <= 2,
                        sizeIcon = 36.dp,
                        cornerRadius = 8.dp,
                        verticalAlignment = Alignment.Top
                    )
                }
                LazyRow123Icons(
                    modifier = Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp),
                    savedRed = savedRed,
                    item = item,
                    isDownloaded = isDownloaded
                )
            }
        }
    }
}

