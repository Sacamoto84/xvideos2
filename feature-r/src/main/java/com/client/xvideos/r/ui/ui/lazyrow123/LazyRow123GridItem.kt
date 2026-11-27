package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.runtime.Composable
import com.client.xvideos.r.model.GifsInfo

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
        host = host,
        isConnected = isConnected,
        isDownloaded = isDownloaded,
        isRunLike = isRunLike,
        onItemClick = onItemClick,
        onRefresh = onRefresh,
        onClickOpenProfile = onClickOpenProfile,
        onTagClick = onTagClick,
    )
}
