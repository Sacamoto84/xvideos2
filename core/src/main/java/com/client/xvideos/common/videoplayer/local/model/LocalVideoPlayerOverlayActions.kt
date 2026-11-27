package com.client.xvideos.common.videoplayer.local.model

import androidx.compose.runtime.Immutable

@Immutable
internal class LocalVideoPlayerOverlayActions(
    val onExitFullScreen: () -> Unit,
    val onRestartPlayback: () -> Unit,
    val onToggleFullScreen: () -> Unit,
    val onScrub: (SeekScrub?) -> Unit,
    val onToggleMute: (() -> Unit)?,
    val onSpeedMenuExpandedChange: (Boolean) -> Unit = {},
)
