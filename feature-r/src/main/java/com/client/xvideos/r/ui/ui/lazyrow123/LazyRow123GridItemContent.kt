package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideo
import com.client.xvideos.r.ui.video.player_row_mini.RedUrlVideoImageAndLongClick

@Composable
internal fun LazyRow123GridItemContent(
    item: GifsInfo,
    index: Int,
    host: LazyRow123Host,
    isConnected: Boolean,
    isDownloaded: Boolean,
    isRunLike: Boolean,
    onItemClick: () -> Unit,
    onRefresh: () -> Unit,
    onClickOpenProfile: (String) -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow123GridItemContentStateless(
        item = item,
        visibleProfileInfo = host.visibleProfileInfo,
        columns = host.columns,
        onClickOpenProfile = onClickOpenProfile,
        onTagClick = onTagClick,
        modifier = modifier,
        videoImage = { onVideo ->
            RedUrlVideoImageAndLongClick(
                item = item,
                index = index,
                onLongClick = onItemClick,
                onVideo = onVideo,
                isVisibleView = false,
                isVisibleDuration = false,
                play = false,
                isNetConnected = isConnected,
                onFullScreen = onItemClick,
                downloadRed = { host.downloadRed },
            )
        },
        expandMenu = {
            ExpandMenuVideo(
                item = item,
                onRunLike = { if (isRunLike) onRefresh() },
                onRefresh = onRefresh,
                isCollection = host.isCollection,
                block = { host.block },
                redApi = { host.redApi },
                savedRed = { host.savedRed },
                downloadRed = { host.downloadRed },
                accountLikes = host.isAccountLikes,
                accountCollectionId = host.accountCollectionId,
            )
        },
        icons = {
            LazyRow123Icons(
                modifier = Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp),
                savedRed = { host.savedRed },
                item = item,
                isDownloaded = isDownloaded
            )
        }
    )
}
