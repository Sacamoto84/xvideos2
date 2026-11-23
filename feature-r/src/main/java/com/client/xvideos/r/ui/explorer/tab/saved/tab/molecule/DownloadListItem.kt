package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.formatBytesOrDefault
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.SavedItemActionsMenu
import com.client.xvideos.r.ui.video.player_row_mini.RED_VIDEO_PREVIEW_ASPECT_RATIO

@Composable
internal fun DownloadListItem(
    item: GifsInfo,
    view: DownloadView,
    videoSize: Long?,
    onItemClick: (GifsInfo) -> Unit,
    onShareClick: (GifsInfo) -> Unit,
    onDeleteClick: (GifsInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewPath = if (LocalInspectionMode.current) null else remember(item.userName, item.id) {
        "${AppPath.r_cache_download}/${item.userName}/${item.id}.jpg"
    }
    val size = formatBytesOrDefault(videoSize, "Нет видео")
    val shape = RoundedCornerShape(8.dp)
    val card = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(Theme.tabLevel3)
        .border(1.dp, Theme.R.colorBorderGray, shape)
        .clickable { onItemClick(item) }
    val actions: @Composable (Modifier) -> Unit = { actionsModifier ->
        DownloadActions(
            modifier = actionsModifier,
            iconModifier = if (view == DownloadView.GRID) Modifier.offset(x = 8.dp) else Modifier,
            onOpen = { onItemClick(item) },
            onShare = { onShareClick(item) },
            onDelete = { onDeleteClick(item) },
        )
    }

    if (view == DownloadView.GRID) {
        Column(card) {
            Box(Modifier.fillMaxWidth().aspectRatio(RED_VIDEO_PREVIEW_ASPECT_RATIO)) {
                DownloadThumbnail(previewPath, Modifier.fillMaxSize())
                actions(Modifier.align(Alignment.TopEnd))
            }
            DownloadSize(size, Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
    } else {
        val thumbnailSize = 96.dp
        Row(
            modifier = card.heightIn(min = thumbnailSize),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DownloadThumbnail(
                previewPath,
                Modifier.width(thumbnailSize).aspectRatio(1f),
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DownloadSize(size)
                DownloadMetadata(item.userName)
                DownloadMetadata("ID: ${item.id}")
            }
            actions(Modifier)
        }
    }
}

@Composable
private fun DownloadThumbnail(path: String?, modifier: Modifier) {
    if (path == null) {
        Box(modifier.background(Color.Gray))
    } else {
        UrlImage(path, modifier = modifier, contentScale = ContentScale.Crop, loadIndicator = false)
    }
}

@Composable
private fun DownloadSize(size: String, modifier: Modifier = Modifier) {
    Text(size, modifier = modifier, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun DownloadMetadata(text: String) {
    Text(text, color = Color.LightGray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun DownloadActions(
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    SavedItemActionsMenu(modifier = modifier, iconModifier = iconModifier) { dismiss ->
        ExpandMenuActionItem(Icons.Outlined.Fullscreen, "Открыть во весь экран") {
            dismiss()
            onOpen()
        }
        ExpandMenuActionItem(Icons.Outlined.Share, "Поделиться") {
            dismiss()
            onShare()
        }
        ExpandMenuActionItem(Icons.Outlined.Delete, "Удалить загрузку") {
            dismiss()
            onDelete()
        }
    }
}
