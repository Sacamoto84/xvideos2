package com.client.xvideos.r.ui.fullscreen.bottom_bar

import androidx.compose.runtime.Immutable
import com.client.xvideos.common.videoplayer.model.PlayerSpeed

@Immutable
data class FeedPlaybackState(
    val timeA: Float,
    val timeB: Float,
    val enableAB: Boolean,
    val play: Boolean,
    val mute: Boolean,
    val speed: PlayerSpeed = PlayerSpeed.DEFAULT,
)

@Immutable
data class FeedPlaybackActions(
    val onSetTimeA: () -> Unit,
    val onSetTimeB: () -> Unit,
    val onToggleAB: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onRewind: () -> Unit,
    val onForward: () -> Unit,
    val onToggleMute: () -> Unit,
    val onSpeedChange: (PlayerSpeed) -> Unit,
)
