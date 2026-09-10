package com.client.xvideos.l.ui.screens.screenFullScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lAnimationVideoUrl
import com.client.xvideos.l.model.lFullScreenImageUrls
import com.client.xvideos.l.model.lPreviewImageUrl
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Одна страница полноэкранного пейджера: видео или картинка с зумом.
 *
 * Выделено из `L_FullScreenImage.kt` (было 800 строк). Тело функции не менялось
 * — перенос дословный.
 */
@Composable
internal fun LFullScreenPage(
    pageItem: PicsDetails,
    page: Int,
    currentIndex: Int,
    /**
     * Только для settledPage — он меняется раз в свайп.
     * Ничего, что обновляется каждый кадр (currentPageOffsetFraction,
     * currentPage вместе с ним), здесь читать нельзя: страница уйдёт
     * в рекомпозицию на каждом кадре прокрутки.
     */
    pagerState: PagerState,
    rotate: Boolean,
    albumName: String,
    autoPlay: Boolean,
    videoMuted: Boolean,
    seekDragEnabled: Boolean,
    onToggleFullScreen: () -> Unit
) {
    val zoomState = rememberZoomState()
    val coroutineScope = rememberCoroutineScope()
    val isCurrentPage = currentIndex == page

    // derivedStateOf, а не settledIndex параметром: чтение settledPage в scope
    // Content рекомпозило весь экран на каждый settle. Здесь рекомпозятся только
    // те страницы, у которых флаг реально поменялся.
    val isSettledPage by remember(page, pagerState) {
        derivedStateOf { pagerState.settledPage == page }
    }

    // Страница ушла из фокуса — снимаем зум. Пейджер держит соседние страницы
    // живыми, а увеличенная картинка рисуется graphicsLayer'ом без clip и
    // налезала на текущую страницу.
    // Ключ именно settled, а не current: currentPage флипается на середине свайпа,
    // и reset() (это snapTo, не анимация) схлопывал картинку прямо на глазах.
    LaunchedEffect(isSettledPage) { if (!isSettledPage) zoomState.reset() }

    // clipToBounds по границам страницы: зум и поворот (rotationZ + scale в
    // UrlImage) рисуют за пределами layout-границ, из-за чего соседние страницы
    // накладывались друг на друга при прокрутке.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(
                if (rotate) pageItem.height.toFloat() / pageItem.width
                else pageItem.width.toFloat() / pageItem.height,
                matchHeightConstraintsFirst = false
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val videoUrl = pageItem.lAnimationVideoUrl()
            if (videoUrl != null) {
                LFullScreenVideo(
                    url = videoUrl,
                    previewUrl = pageItem.lPreviewImageUrl("large_thumbnail"),
                    albumName = albumName,
                    autoPlay = autoPlay,
                    isCurrentPage = isCurrentPage,
                    isPlayerActive = isSettledPage,
                    isMuted = videoMuted,
                    seekDragEnabled = seekDragEnabled,
                    rotate = rotate,
                    modifier = Modifier.fillMaxSize(),
                    onTap = onToggleFullScreen
                )
            } else {
                val imageUrls = remember(pageItem.url_to_original, pageItem.thumbnails) {
                    pageItem.lFullScreenImageUrls()
                }
                var imageUrlIndex by remember(imageUrls) { mutableIntStateOf(0) }
                val imageUrl = imageUrls.getOrNull(imageUrlIndex).orEmpty()

                if (imageUrl.isNotBlank()) {
                    UrlImage(
                        rotate = rotate,
                        contentScale = ContentScale.Fit,
                        url = imageUrl,
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
                                onTap = { onToggleFullScreen() }
                            ),
                        onSuccess = { },
                        onFailure = {
                            if (imageUrlIndex < imageUrls.lastIndex) {
                                imageUrlIndex += 1
                                timber.log.Timber.w("!!! L fullscreen image fallback ${imageUrlIndex}/${imageUrls.lastIndex}: ${imageUrls[imageUrlIndex]}")
                            }
                        },
                        albumName = albumName,
                        autoPlay = autoPlay,
                        isAnimated = pageItem.is_animated,
                        isVisible = isCurrentPage,
                        isFullScreen = true
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет ссылки на изображение", color = Color.Gray)
                    }
                }
            }
        }
    }
    }
}
