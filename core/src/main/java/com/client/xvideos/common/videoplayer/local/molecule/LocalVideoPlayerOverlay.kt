package com.client.xvideos.common.videoplayer.local.molecule

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.local.atom.ResumePlaybackPill
import com.client.xvideos.common.videoplayer.local.model.LocalVideoPlayerOverlayActions
import com.client.xvideos.common.videoplayer.local.model.SeekScrub
import com.client.xvideos.common.videoplayer.ui.component.PlayPauseFeedback
import com.client.xvideos.common.videoplayer.ui.component.PlayPauseFeedbackState

@OptIn(UnstableApi::class)
@Composable
internal fun BoxScope.LocalVideoPlayerOverlay(
    host: MediaPlayerHost,
    playbackFeedback: PlayPauseFeedbackState,
    isFullScreen: Boolean,
    areControlsVisible: Boolean,
    title: String,
    resumeNoticeText: String?,
    storyboard: ImageBitmap?,
    scrubProvider: () -> SeekScrub?,
    muted: Boolean,
    actions: LocalVideoPlayerOverlayActions,
) {
    PlayPauseFeedback(playbackFeedback, Modifier.align(Alignment.Center))

    if (isFullScreen) {
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            LocalPlayerFullScreenTopBar(title = title, onBack = actions.onExitFullScreen)
        }
    }

    AnimatedVisibility(
        visible = resumeNoticeText != null && (!isFullScreen || areControlsVisible),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 76.dp)
    ) {
        resumeNoticeText?.let { notice ->
            ResumePlaybackPill(
                text = notice,
                onRestart = actions.onRestartPlayback
            )
        }
    }

    SeekPreviewOverlay(
        storyboard = storyboard,
        scrubProvider = scrubProvider,
        durationSec = host.totalTime
    )

    AnimatedVisibility(
        visible = !isFullScreen || areControlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        PlayerBottomBar(
            host = host,
            isFullScreen = isFullScreen,
            onFullScreen = actions.onToggleFullScreen,
            onScrub = actions.onScrub,
            muted = muted,
            onToggleMute = actions.onToggleMute,
            onSpeedMenuExpandedChange = actions.onSpeedMenuExpandedChange,
            onTogglePlayPause = playbackFeedback::togglePlayback,
        )
    }
}

@Preview
@Composable
private fun LocalVideoPlayerOverlayPreview() {
    Box(Modifier.fillMaxSize()) {
        ResumePlaybackPill(text = "Возобновлено с 0:45", onRestart = {})
    }
}
