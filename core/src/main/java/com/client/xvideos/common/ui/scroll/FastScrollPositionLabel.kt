package com.client.xvideos.common.ui.scroll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.scrollFabSurface
import dev.chrisbanes.haze.HazeState

private const val POSITION_LABEL_FADE_MILLIS = 150

@Composable
internal fun FastScrollPositionLabel(
    positionLabel: String?,
    offsetY: () -> Int,
    effect: ScrollButtonEffect,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    var lastPositionLabel by remember { mutableStateOf<String?>(null) }
    if (positionLabel != null) {
        SideEffect { lastPositionLabel = positionLabel }
    }
    AnimatedVisibility(
        visible = positionLabel != null,
        modifier = modifier.offset { IntOffset(0, offsetY()) },
        enter = fadeIn(tween(POSITION_LABEL_FADE_MILLIS)),
        exit = fadeOut(tween(POSITION_LABEL_FADE_MILLIS)),
        label = "scroll position",
    ) {
        val text = positionLabel ?: lastPositionLabel
        if (text != null) {
            Text(
                text = text,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .scrollFabSurface(effect = effect, hazeState = hazeState)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = Theme.ScrollFab.contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun FastScrollPositionLabelPreview() {
    FastScrollPositionLabel(
        positionLabel = "15 / 120",
        offsetY = { 0 },
        effect = ScrollButtonEffect.FLAT,
        hazeState = HazeState(),
    )
}
