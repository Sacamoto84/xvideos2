package com.client.xvideos.x.screens.tags.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX

/**
 * Ячейка сетки видео в выдаче по тегу.
 */
@Composable
fun TagGridCell(
    cell: ItemsX,
    onOpenVideo: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(cell, onOpenVideo) { { onOpenVideo(cell) } }
    Box(
        modifier = modifier
            .aspectRatio(352f / 198f)
            .padding(1.dp)
            .background(Color.DarkGray)
    ) {
        UrlVideoImageAndLongClickX(
            cell,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun TagGridCellPreview() {
    TagGridCell(
        cell = ItemsX(
            id = 101L,
            title = "Preview Tag Video",
            duration = "10:00",
            views = "100K",
            channel = "Studio",
            href = "/video",
            nameProfile = "Studio",
            linkProfile = "/studio",
        ),
        onOpenVideo = {}
    )
}
