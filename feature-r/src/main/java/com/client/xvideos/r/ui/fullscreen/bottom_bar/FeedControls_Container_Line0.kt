package com.client.xvideos.r.ui.fullscreen.bottom_bar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toTwoDecimalPlacesWithColon
import com.client.xvideos.feature.r.R
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreenSM

@Immutable
data class FeedPlaybackState(
    val timeA: Float,
    val timeB: Float,
    val enableAB: Boolean,
    val play: Boolean,
    val mute: Boolean,
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
)

@Composable
private fun Divider() {
    Spacer(modifier = Modifier.height(8.dp).width(2.dp).background(Color.DarkGray))
}

@Composable
private fun TimeMarkerButton(
    label: String,
    time: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(46.dp)
            .width(46.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            time.toTwoDecimalPlacesWithColon(),
            style = TextStyle(
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = Theme.R.fontFamilyPopinsRegular,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            label,
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = Theme.R.fontFamilyPopinsRegular,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AbToggleButton(
    enableAB: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.rg_button),
                contentDescription = if (enableAB) "Выключить повтор отрезка A-B" else "Включить повтор отрезка A-B",
                tint = if (enableAB) Color.Green else Color.LightGray
            )
            Text(
                "AB",
                color = if (enableAB) Color.Green else Color.LightGray,
                fontSize = 8.sp,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )
        }
    }
}

@Composable
private fun PlayPauseButton(
    play: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(if (play) R.drawable.select_1 else R.drawable.rg_button),
            contentDescription = if (play) "Пауза" else "Воспроизведение",
            tint = Color.White,
            modifier = Modifier.rotate(if (play) 90f else 0f)
        )
    }
}

@Composable
private fun SeekButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White
        )
    }
}

@Composable
private fun MuteButton(
    mute: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .width(46.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val icon = if (mute) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp
        Icon(
            icon,
            contentDescription = if (mute) "Включить звук" else "Выключить звук",
            tint = if (mute) Color.Gray else Color.White
        )
    }
}

@Composable
fun FeedControls_Container_Line0(
    state: FeedPlaybackState,
    actions: FeedPlaybackActions,
    modifier: Modifier = Modifier,
) {
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
        Divider()
        TimeMarkerButton(
            label = "B",
            time = state.timeB,
            onClick = actions.onSetTimeB
        )
        Divider()
        AbToggleButton(
            enableAB = state.enableAB,
            onClick = actions.onToggleAB
        )
        Divider()
        PlayPauseButton(
            play = state.play,
            onClick = actions.onTogglePlay
        )
        Divider()
        SeekButton(
            iconRes = R.drawable.exo_icon_rewind,
            contentDescription = "Перемотать назад",
            onClick = actions.onRewind
        )
        Divider()
        SeekButton(
            iconRes = R.drawable.exo_icon_fastforward,
            contentDescription = "Перемотать вперёд",
            onClick = actions.onForward
        )
        Divider()
        MuteButton(
            mute = state.mute,
            onClick = actions.onToggleMute
        )
    }
}

@Composable
fun FeedControls_Container_Line0(
    vm: ScreenRedFullScreenSM,
    modifier: Modifier = Modifier,
) {
    val actions = remember(vm) {
        FeedPlaybackActions(
            onSetTimeA = vm::setTimeA,
            onSetTimeB = vm::setTimeB,
            onToggleAB = vm::toggleAB,
            onTogglePlay = vm::togglePlay,
            onRewind = { vm.rewind() },
            onForward = { vm.forward() },
            onToggleMute = vm::toggleMute,
        )
    }

    FeedControls_Container_Line0(
        state = FeedPlaybackState(
            timeA = vm.timeA,
            timeB = vm.timeB,
            enableAB = vm.enableAB,
            play = vm.play,
            mute = vm.mute,
        ),
        actions = actions,
        modifier = modifier,
    )
}


