package com.client.xvideos.common.ui.statusbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Заявка полноэкранного экрана: пока он в композиции, статус-бар скрыт.
 */
@Composable
fun HideStatusBarEffect() {
    val requests = LocalStatusBarRequests.current
    DisposableEffect(requests) {
        requests.acquireHide()
        onDispose { requests.releaseHide() }
    }
}
