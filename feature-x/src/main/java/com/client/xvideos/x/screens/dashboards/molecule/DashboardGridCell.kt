package com.client.xvideos.x.screens.dashboards.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.icons.IconFavorite18
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.dashboards.atom.ShadowedDurationText
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu

@Composable
fun DashboardGridCell(
    cell: ItemsX,
    isFavorite: Boolean,
    openVideoPlayer: (ItemsX) -> Unit,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(cell, openVideoPlayer) { { openVideoPlayer(cell) } }
    val handleFavoriteAdd = remember(cell, onFavoriteAdd) { { onFavoriteAdd(cell) } }
    val handleFavoriteRemove = remember(cell, onFavoriteRemove) { { onFavoriteRemove(cell) } }
    val handleDownload = remember(cell, onDownload) { { onDownload(cell) } }
    val handleSaveToGallery = remember(cell, onSaveToGallery) { { onSaveToGallery(cell) } }
    val durationText = remember(cell.duration) { cell.duration.trim().removeSuffix(".") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(352f / 198f)
            .padding(1.dp)
            .background(Color.DarkGray)
    ) {
        UrlVideoImageAndLongClickX(
            cell,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
        ) {
            if (durationText.isNotEmpty()) {
                ShadowedDurationText(durationText = durationText)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color(0x60000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cell.channel,
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Row(modifier = Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.End) {
                if (isFavorite) {
                    IconFavorite18(Modifier.padding(bottom = 6.dp, end = 6.dp))
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                X_DashboardExpandMenu(
                    isFavorite = isFavorite,
                    onFavoriteAdd = handleFavoriteAdd,
                    onFavoriteRemove = handleFavoriteRemove,
                    onDownload = handleDownload,
                    onSaveToGallery = handleSaveToGallery,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DashboardGridCellPreview() {
    DashboardGridCell(
        cell = ItemsX(
            id = 1L,
            title = "Preview Video",
            duration = "10:20",
            channel = "ChannelName"
        ),
        isFavorite = true,
        openVideoPlayer = {},
        onFavoriteAdd = {},
        onFavoriteRemove = {},
        onDownload = {},
        onSaveToGallery = {}
    )
}
