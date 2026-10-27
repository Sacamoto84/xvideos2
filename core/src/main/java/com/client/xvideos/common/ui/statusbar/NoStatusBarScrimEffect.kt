package com.client.xvideos.common.ui.statusbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Заявка экрана с тёмной полосой под статус-баром: пока он в композиции,
 * подложка под баром не рисуется. Бар при этом остаётся видимым.
 */
@Composable
fun NoStatusBarScrimEffect() {
    val requests = LocalStatusBarRequests.current
    DisposableEffect(requests) {
        requests.acquireNoScrim()
        onDispose { requests.releaseNoScrim() }
    }
}
