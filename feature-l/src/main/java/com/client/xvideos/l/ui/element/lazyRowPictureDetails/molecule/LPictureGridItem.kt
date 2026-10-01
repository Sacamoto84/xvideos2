package com.client.xvideos.l.ui.element.lazyRowPictureDetails.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lAnimationVideoUrl
import com.client.xvideos.l.model.lPreviewImageUrl
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.atom.AnimatedVideoPlaceholder

@Composable
fun LPictureGridItem(
    item: PicsDetails,
    index: Int,
    thumbnailsSize: String,
    isVisible: Boolean,
    albumName: String,
    onOpenFullScreen: () -> Unit,
    menuContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val aspect = if (item.width > 0 && item.height > 0) {
            item.width.toFloat() / item.height
        } else {
            1f
        }

        val previewUrl = item.lPreviewImageUrl(thumbnailsSize)
        val videoUrl = item.lAnimationVideoUrl()
        var playInline by remember(item.url_to_original, item.url_to_video) { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .padding(1.dp)
                .aspectRatio(aspect)
                .border(width = 0.5.dp, color = Theme.tabLevel4, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Theme.tabLevel1)
        ) {
            if (playInline && videoUrl != null) {
                LInlineAnimationVideo(
                    url = videoUrl,
                    previewUrl = previewUrl,
                    albumName = albumName,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (previewUrl.isNotBlank() && !previewUrl.isLVideoFileUrl()) {
                UrlImage(
                    url = previewUrl,
                    contentScale = ContentScale.FillHeight,
                    urlGif = item.url_to_original,
                    modifier = Modifier.fillMaxSize(),
                    albumName = albumName,
                    isAnimated = false,
                    backgroung = Theme.tabLevel1,
                    isVisible = isVisible
                )
            } else {
                AnimatedVideoPlaceholder(modifier = Modifier.fillMaxSize())
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .combinedClickable(
                        onClick = {
                            if (videoUrl != null) {
                                playInline = true
                            } else {
                                onOpenFullScreen()
                            }
                        },
                        onLongClick = {
                            if (videoUrl != null) {
                                onOpenFullScreen()
                            }
                        }
                    )
            )

            if (videoUrl != null && !playInline) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }
        }

        Text(
            index.toString(),
            modifier = Modifier.padding(start = 4.dp).align(Alignment.TopStart),
            color = Theme.L.textColor,
            style = Theme.L.Type.mediaIndex
        )

        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            menuContent()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun LPictureGridItemPreview() {
    LPictureGridItem(
        item = PicsDetails(
            url_to_original = "",
            url_to_video = null,
            width = 300,
            height = 400
        ),
        index = 1,
        thumbnailsSize = "normal",
        isVisible = true,
        albumName = "Preview Album",
        onOpenFullScreen = {},
        menuContent = {}
    )
}
