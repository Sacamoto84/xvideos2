package com.client.xvideos.n.ui.screens.reader.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.n.model.NPage
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Одиночная страница изображения внутри ридера манги с поддержкой жестов зума и масштабирования.
 */
@Composable
fun ReaderImagePage(
    page: NPage,
    mediaId: Long,
    modifier: Modifier = Modifier,
    isSettledPage: Boolean = true,
    onTap: (() -> Unit)? = null
) {
    val zoomState = rememberZoomState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isSettledPage) {
        if (!isSettledPage) {
            zoomState.reset()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        UrlImage(
            url = page.imageUrl(mediaId),
            modifier = Modifier
                .fillMaxSize()
                .zoomable(
                    zoomState = zoomState,
                    enableOneFingerZoom = false,
                    onDoubleTap = { position ->
                        coroutineScope.launch {
                            if (zoomState.scale > 1.0f) {
                                zoomState.changeScale(1.0f, Offset.Zero)
                            } else {
                                zoomState.changeScale(2.5f, position)
                            }
                        }
                    },
                    onTap = { onTap?.invoke() }
                ),
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
