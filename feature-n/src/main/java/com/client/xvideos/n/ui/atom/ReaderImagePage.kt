package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.n.model.NPage

/**
 * Одиночная страница изображения внутри ридера манги.
 */
@Composable
fun ReaderImagePage(
    page: NPage,
    mediaId: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        UrlImage(
            url = page.imageUrl(mediaId),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Preview
@Composable
private fun ReaderImagePagePreview() {
    ReaderImagePage(
        page = NPage(pageNumber = 1, ext = "jpg"),
        mediaId = 12345L
    )
}
