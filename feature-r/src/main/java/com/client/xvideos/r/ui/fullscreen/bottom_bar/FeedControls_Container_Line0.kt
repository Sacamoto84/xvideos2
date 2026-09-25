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
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.ui.component.PlaybackSpeedMenu
import com.client.xvideos.feature.r.R
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreenSM

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

private val DIVIDER_MODIFIER = Modifier.height(8.dp).width(2.dp).background(Color.DarkGray)
private val BUTTON_BOX_MODIFIER = Modifier.height(46.dp).width(46.dp)
private val ICON_BUTTON_SIZE_MODIFIER = Modifier.size(46.dp)
private val ROW_BASE_MODIFIER = Modifier.fillMaxWidth().height(48.dp)
private val PADDING_4_MODIFIER = Modifier.padding(horizontal = 4.dp)

private val COLOR_WHITE = Color.White
private val COLOR_GRAY = Color.Gray
private val COLOR_GREEN = Color.Green
private val COLOR_LIGHT_GRAY = Color.LightGray

private val ICON_VOLUME_OFF = Icons.AutoMirrored.Filled.VolumeOff
private val ICON_VOLUME_UP = Icons.AutoMirrored.Filled.VolumeUp

private val ALIGN_CENTER = Alignment.Center
private val ALIGN_CENTER_HORIZONTALLY = Alignment.CenterHorizontally
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val ROW_HORIZONTAL_ARRANGEMENT = Arrangement.SpaceBetween
private val COLUMN_VERTICAL_ARRANGEMENT = Arrangement.Center

private const val ROTATION_0 = 0f
private const val ROTATION_90 = 90f
private val PLAY_ROTATED_MODIFIER = Modifier.rotate(ROTATION_90)
private val PLAY_DEFAULT_MODIFIER = Modifier.rotate(ROTATION_0)

@Composable
private fun Divider() {
    Spacer(modifier = DIVIDER_MODIFIER)
}

@Composable
private fun TimeMarkerButton(
    label: String,
    time: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = remember {
        TextStyle(
            color = COLOR_WHITE,
            fontSize = 10.sp,
            fontFamily = Theme.R.fontFamilyPopinsRegular,
            textAlign = TextAlign.Center
        )
    }
    Column(
        modifier = modifier
            .then(BUTTON_BOX_MODIFIER)
            .clickable(onClick = onClick),
        verticalArrangement = COLUMN_VERTICAL_ARRANGEMENT,
        horizontalAlignment = ALIGN_CENTER_HORIZONTALLY
    ) {
        BasicText(
            time.toTwoDecimalPlacesWithColon(),
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            label,
            color = COLOR_WHITE,
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
        modifier = modifier.then(ICON_BUTTON_SIZE_MODIFIER)
    ) {
        Box(contentAlignment = ALIGN_CENTER) {
            val color = if (enableAB) COLOR_GREEN else COLOR_LIGHT_GRAY
            Icon(
                painter = painterResource(R.drawable.rg_button),
                contentDescription = if (enableAB) "Выключить повтор отрезка A-B" else "Включить повтор отрезка A-B",
                tint = color
            )
            Text(
                "AB",
                color = color,
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
            tint = COLOR_WHITE,
            modifier = if (play) PLAY_ROTATED_MODIFIER else PLAY_DEFAULT_MODIFIER
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
            tint = COLOR_WHITE
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
            .then(BUTTON_BOX_MODIFIER)
            .clickable(onClick = onClick),
        contentAlignment = ALIGN_CENTER
    ) {
        val icon = if (mute) ICON_VOLUME_OFF else ICON_VOLUME_UP
        Icon(
            icon,
            contentDescription = if (mute) "Включить звук" else "Выключить звук",
            tint = if (mute) COLOR_GRAY else COLOR_WHITE
        )
    }
}

@Composable
fun FeedControls_Container_Line0(
    state: FeedPlaybackState,
    actions: FeedPlaybackActions,
    modifier: Modifier = Modifier,
) {
    val triggerContent: @Composable (() -> Unit) -> Unit = remember(state.speed.displayName) {
        { onClick ->
            Box(
                modifier = BUTTON_BOX_MODIFIER
                    .clickable(onClick = onClick),
                contentAlignment = ALIGN_CENTER
            ) {
                Text(
                    text = state.speed.displayName,
                    color = COLOR_WHITE,
                    fontSize = 12.sp,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Row(
        modifier = modifier
            .then(ROW_BASE_MODIFIER)
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = ROW_HORIZONTAL_ARRANGEMENT,
        verticalAlignment = ROW_VERTICAL_ALIGNMENT
    ) {
        TimeMarkerButton(
            label = "A",
            time = state.timeA,
            onClick = actions.onSetTimeA,
            modifier = PADDING_4_MODIFIER
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
        Divider()
        PlaybackSpeedMenu(
            currentSpeed = state.speed,
            onSpeedSelected = actions.onSpeedChange,
            trigger = triggerContent
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
            onSpeedChange = { vm.speed = it },
        )
    }

    FeedControls_Container_Line0(
        state = FeedPlaybackState(
            timeA = vm.timeA,
            timeB = vm.timeB,
            enableAB = vm.enableAB,
            play = vm.play,
            mute = vm.mute,
            speed = vm.speed,
        ),
        actions = actions,
        modifier = modifier,
    )
}


