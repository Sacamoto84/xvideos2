package com.client.xvideos.r.ui.explorer.tab.saved.tab.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage

@Composable
internal fun DownloadThumbnail(path: String?, modifier: Modifier = Modifier) {
    if (path == null) {
        Box(modifier.background(Color.Gray))
    } else {
        UrlImage(path, modifier = modifier, contentScale = ContentScale.Crop, loadIndicator = false)
    }
}

@Preview
@Composable
private fun DownloadThumbnailPreview() {
    DownloadThumbnail(path = null, modifier = Modifier.size(72.dp))
}
