package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.videoplayer.ui.component.PlaybackSpeedMenu
import com.client.xvideos.feature.r.R
import com.client.xvideos.r.ui.fullscreen.atom.AbToggleButton
import com.client.xvideos.r.ui.fullscreen.atom.FeedControlDivider
import com.client.xvideos.r.ui.fullscreen.atom.MuteButton
import com.client.xvideos.r.ui.fullscreen.atom.PlayPauseButton
import com.client.xvideos.r.ui.fullscreen.atom.SeekButton
import com.client.xvideos.r.ui.fullscreen.atom.TimeMarkerButton
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedPlaybackActions
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedPlaybackState

@Composable
fun FeedControls_Container_Line0(
    state: FeedPlaybackState,
    actions: FeedPlaybackActions,
    modifier: Modifier = Modifier,
) {
    val triggerContent: @Composable (() -> Unit) -> Unit = remember(state.speed.displayName) {
        { onClick ->
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.speed.displayName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeMarkerButton(
            label = "A",
            time = state.timeA,
            onClick = actions.onSetTimeA,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        FeedControlDivider()
        TimeMarkerButton(
            label = "B",
            time = state.timeB,
            onClick = actions.onSetTimeB
        )
        FeedControlDivider()
        AbToggleButton(
            enableAB = state.enableAB,
            onClick = actions.onToggleAB
        )
        FeedControlDivider()
        PlayPauseButton(
            play = state.play,
            onClick = actions.onTogglePlay
        )
        FeedControlDivider()
        SeekButton(
            iconRes = R.drawable.exo_icon_rewind,
            contentDescription = "Перемотать назад",
            onClick = actions.onRewind
        )
        FeedControlDivider()
        SeekButton(
            iconRes = R.drawable.exo_icon_fastforward,
            contentDescription = "Перемотать вперёд",
            onClick = actions.onForward
        )
        FeedControlDivider()
        MuteButton(
            mute = state.mute,
            onClick = actions.onToggleMute
        )
        FeedControlDivider()
        PlaybackSpeedMenu(
            currentSpeed = state.speed,
            onSpeedSelected = actions.onSpeedChange,
            trigger = triggerContent
        )
    }
}

@Preview
@Composable
private fun FeedControls_Container_Line0Preview() {
    FeedControls_Container_Line0(
        state = FeedPlaybackState(
            timeA = 1.23f,
            timeB = 4.56f,
            enableAB = true,
            play = true,
            mute = false
        ),
        actions = FeedPlaybackActions(
            onSetTimeA = {},
            onSetTimeB = {},
            onToggleAB = {},
            onTogglePlay = {},
            onRewind = {},
            onForward = {},
            onToggleMute = {},
            onSpeedChange = {}
        )
    )
}
