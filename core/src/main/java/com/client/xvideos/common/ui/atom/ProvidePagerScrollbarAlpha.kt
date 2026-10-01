package com.client.xvideos.common.ui.atom

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.client.xvideos.common.ui.composition.LocalScrollbarAlpha
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

/**
 * Оборачивает содержимое пейджера и скрывает скроллбары во время движения пейджера.
 * При начале свайпа скроллбар моментально исчезает (snapTo 0f).
 * При завершении движения плавно проявляется обратно (animateTo 1f).
 */
@Composable
fun ProvidePagerScrollbarAlpha(
    pagerState: PagerState,
    content: @Composable () -> Unit
) {
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.isScrollInProgress || abs(pagerState.currentPageOffsetFraction) > 0.001f
        }.collectLatest { isMoving ->
            if (isMoving) {
                alphaAnim.snapTo(0f)
            } else {
                alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 200))
            }
        }
    }

    val alphaProvider = remember { { alphaAnim.value } }

    CompositionLocalProvider(LocalScrollbarAlpha provides alphaProvider) {
        content()
    }
}

@Preview
@Composable
private fun ProvidePagerScrollbarAlphaPreview() {
    val pagerState = rememberPagerState { 3 }
    ProvidePagerScrollbarAlpha(pagerState = pagerState) {
        Text("Content")
    }
}
