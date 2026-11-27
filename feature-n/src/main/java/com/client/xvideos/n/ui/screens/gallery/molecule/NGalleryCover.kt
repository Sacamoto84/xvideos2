package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NGallery

@Composable
internal fun NGalleryCover(gallery: NGallery, modifier: Modifier = Modifier) {
    val cover = gallery.cover
    val ratio = if (cover.width > 0 && cover.height > 0) {
        (cover.width.toFloat() / cover.height).coerceIn(0.55f, 2f)
    } else {
        137f / 200f
    }
    val shape = RoundedCornerShape(8.dp)
    UrlImage(
        url = gallery.coverUrl,
        modifier = modifier
            .aspectRatio(ratio)
            .clip(shape)
            .background(Theme.tabLevel1)
            .border(1.dp, Theme.Surface.border, shape),
        contentScale = ContentScale.Fit
    )
}

@Preview
@Composable
private fun NGalleryCoverPreview() {
    NGalleryCover(gallery = NGallery(id = 123456L, mediaId = 987654L))
}
