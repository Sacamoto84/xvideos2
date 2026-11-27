package com.client.xvideos.common.videoplayer.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun PlayPauseFeedback(state: PlayPauseFeedbackState, modifier: Modifier = Modifier) {
    val action = state.action ?: return
    key(action) {
        var dismissed by remember { mutableStateOf(false) }
        if (dismissed) return@key
        val visibility = remember {
            MutableTransitionState(false).apply { targetState = true }
        }
        LaunchedEffect(visibility) {
            snapshotFlow { visibility.currentState }.first { it }
            delay(if (action.isPaused) 450L else 200L)
            visibility.targetState = false
            snapshotFlow { visibility.isIdle && !visibility.currentState }.first { it }
            dismissed = true
        }
        AnimatedVisibility(
            visibleState = visibility,
            modifier = modifier,
            enter = scaleIn(
                initialScale = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0 using LinearOutSlowInEasing
                    1.3f at 110 using FastOutSlowInEasing
                    1f at 350
                },
            ),
            exit = fadeOut(tween(150)),
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(Color(0x99000000), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (action.isPaused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PlayPauseFeedbackPreview() {
    val state = remember {
        PlayPauseFeedbackState(togglePlaybackAndGetPaused = { true }).apply { togglePlayback() }
    }
    PlayPauseFeedback(state = state)
}
