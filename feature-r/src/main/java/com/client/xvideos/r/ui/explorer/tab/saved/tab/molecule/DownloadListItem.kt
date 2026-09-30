package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toPrettyCount3
import com.client.xvideos.r.model.GifsInfo
import java.io.File

@Composable
fun DownloadListItem(
    item: GifsInfo,
    onItemClick: (GifsInfo) -> Unit,
    onFullScreenClick: (GifsInfo) -> Unit,
    onShareClick: (GifsInfo) -> Unit,
    onDeleteClick: (GifsInfo) -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
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

@Preview
@Composable
private fun DownloadListItemPreview() {
    DownloadListItem(
        item = GifsInfo(
            id = "test_id",
            userName = "test_user"
        ),
        onItemClick = {},
        onFullScreenClick = {},
        onShareClick = {},
        onDeleteClick = {}
    )
}
