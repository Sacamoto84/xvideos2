package com.client.xvideos.x.screens.saved.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.x.model.ItemsX

@Composable
fun SavedRow(
    item: ItemsX,
    posterUrl: String,
    onPlay: (ItemsX) -> Unit,
    onDelete: (ItemsX) -> Unit,
    onShareP2p: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handlePlay = remember(item, onPlay) { { onPlay(item) } }
    val handleShareP2p = remember(item, onShareP2p) { { onShareP2p(item) } }
    val handleDelete = remember(item, onDelete) { { onDelete(item) } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(352f / 198f)
                .background(Color.DarkGray)
                .clickable(onClick = handlePlay)
        ) {
            UrlImage(url = posterUrl, modifier = Modifier.fillMaxSize())

            // Продолжительность видео в правом верхнем углу.
            Text(
                text = item.duration,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                textAlign = TextAlign.Right,
                fontSize = 14.sp,
                color = Color.White
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.L.grey6),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            IconButton(onClick = handleShareP2p) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "P2P",
                    tint = Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = handleDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Удалить",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun SavedRowPreview() {
    SavedRow(
        item = ItemsX(
            id = 1L,
            title = "Preview Title",
            duration = "10:00"
        ),
        posterUrl = "",
        onPlay = {},
        onDelete = {},
        onShareP2p = {}
    )
}
