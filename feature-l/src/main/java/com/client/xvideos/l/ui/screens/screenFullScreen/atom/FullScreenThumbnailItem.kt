package com.client.xvideos.l.ui.screens.screenFullScreen.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lPreviewImageUrl
import com.client.xvideos.l.model.safeAspectRatio

private val THUMB_CORNER_SHAPE = RoundedCornerShape(4.dp)

@Composable
fun FullScreenThumbnailItem(
    item: PicsDetails,
    isSelected: Boolean,
    albumName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .clip(THUMB_CORNER_SHAPE)
            .aspectRatio(item.safeAspectRatio())
            .clickable(onClick = onClick)
            .border(
                2.dp,
                if (isSelected) Color.Yellow else Color.Transparent,
                THUMB_CORNER_SHAPE
            )
            .padding(2.dp)
    ) {
        val thumbUrl = item.lPreviewImageUrl("large_thumbnail")
        if (thumbUrl.isNotBlank() && !thumbUrl.isLVideoFileUrl()) {
            UrlImage(
                url = thumbUrl,
                modifier = Modifier
                    .clip(THUMB_CORNER_SHAPE)
                    .fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                onSuccess = { },
                albumName = albumName,
                autoPlay = false,
                isAnimated = false,
                sizeButton = 20.dp,
                sizeButtonIcon = 12.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(THUMB_CORNER_SHAPE)
                    .background(Color(0xFF202020))
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Preview
@Composable
private fun FullScreenThumbnailItemPreview() {
    FullScreenThumbnailItem(
        item = PicsDetails(),
        isSelected = true,
        albumName = "test",
        onClick = {}
    )
}
