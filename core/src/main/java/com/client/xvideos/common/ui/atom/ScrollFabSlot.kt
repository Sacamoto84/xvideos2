package com.client.xvideos.common.ui.atom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.theme.Theme
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

@Composable
fun ScrollFabSlot(
    visible: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    contentColor: Color = Theme.ScrollFab.contentColor,
    effect: ScrollButtonEffect = ScrollButtonEffect.FLAT,
) {
    Box(
        modifier = modifier.size(Theme.ScrollFab.size),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FloatingActionButton(
                onClick = onClick,
                containerColor = Theme.ScrollFab.containerColor,
                contentColor = contentColor,
                shape = Theme.ScrollFab.shape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                modifier = Modifier.scrollFabSurface(effect = effect, hazeState = hazeState)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription
                )
            }
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
private val scrollFabBlurStyle: HazeBlurStyle by lazy {
    HazeBlurStyle.then {
        backgroundColor(Theme.ScrollFab.backgroundColor)
        blurRadius(Theme.ScrollFab.blurRadius)
        noiseFactor(Theme.ScrollFab.noiseFactor)
        blurredEdgeTreatment(BlurredEdgeTreatment(Theme.ScrollFab.shape))
    }
}

@OptIn(ExperimentalHazeApi::class)
private val scrollFabGlassStyle: GlassStyle by lazy {
    GlassStyle.regular.then {
        backgroundColor(Theme.ScrollFab.backgroundColor)
        tint(Theme.ScrollFab.tintColor)
        shape(Theme.ScrollFab.shape)
        whitePoint(Theme.ScrollFab.whitePoint)
        specularIntensity(Theme.ScrollFab.specularIntensity)
        ambientResponse(Theme.ScrollFab.ambientResponse)
    }
}

@OptIn(ExperimentalHazeApi::class)
internal fun Modifier.scrollFabSurface(
    effect: ScrollButtonEffect,
    hazeState: HazeState,
): Modifier {
    val shape = Theme.ScrollFab.shape
    val surface = when (effect) {
        ScrollButtonEffect.FLAT -> Modifier.background(Theme.ScrollFab.backgroundColor, shape)
        ScrollButtonEffect.BLUR -> Modifier.hazeBlur(
            input = HazeInput.Sources(hazeState),
            style = scrollFabBlurStyle,
        )
        ScrollButtonEffect.GLASS -> Modifier.hazeGlass(
            input = HazeInput.Sources(hazeState),
            style = scrollFabGlassStyle,
        )
    }
    return clip(shape)
        .then(surface)
        .border(Theme.ScrollFab.borderWidth, Theme.ScrollFab.glassBorder, shape)
}

@Preview
@Composable
private fun ScrollFabSlotPreview() {
    ScrollFabSlot(
        visible = true,
        onClick = {},
        icon = Icons.Default.KeyboardArrowUp,
        contentDescription = "Вверх",
        hazeState = remember { HazeState() }
    )
}
