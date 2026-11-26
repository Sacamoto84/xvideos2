package com.client.xvideos.common.videoplayer.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost

class PlayPauseFeedbackState(
    private val togglePlaybackAndGetPaused: () -> Boolean,
    private val enabled: Boolean = true,
) {
    constructor(host: MediaPlayerHost, enabled: Boolean) : this(
        togglePlaybackAndGetPaused = {
            host.togglePlayPause()
            host.isPaused
        },
        enabled = enabled,
    )

    internal var action by mutableStateOf<Action?>(null)
        private set

    fun togglePlayback() {
        val isPaused = togglePlaybackAndGetPaused()
        if (enabled) action = Action(isPaused)
    }

    fun clear() {
        action = null
    }

    // У каждого нажатия свой ключ, даже если ролик успел сам встать на паузу.
    internal class Action(val isPaused: Boolean)
}
