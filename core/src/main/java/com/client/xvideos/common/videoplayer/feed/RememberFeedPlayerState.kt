package com.client.xvideos.common.videoplayer.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect

@Composable
fun rememberFeedPlayerState(
    poolCapacity: Int = FeedPlayerState.DEFAULT_POOL_CAPACITY,
): FeedPlayerState {
    val context = LocalContext.current
    val state = remember(context, poolCapacity) { FeedPlayerState(context, poolCapacity) }
    // Освобождение объявлено первым: эффекты снимаются в обратном порядке, и
    // foreground-режим выключается у ещё живого пула. Раньше пул освобождался
    // раньше, а режим выключался уже на освобождённом.
    DisposableEffect(state) { onDispose { state.release() } }
    LifecycleStartEffect(state) {
        state.setForegroundMode(true)
        onStopOrDispose { state.setForegroundMode(false) }
    }
    return state
}
