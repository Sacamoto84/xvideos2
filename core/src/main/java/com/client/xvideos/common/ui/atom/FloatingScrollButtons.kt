package com.client.xvideos.common.ui.atom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

/**
 * Плавающие кнопки быстрой прокрутки ("Вверх" и "Вниз") с эффектом матового стекла (Haze).
 * Кнопки закреплены на фиксированных слотах (Box с фиксированным размером 56.dp),
 * что исключает смещение одной кнопки при исчезновении другой.
 */
@Composable
fun FloatingScrollButtons(
    showScrollToTop: Boolean,
    showScrollToBottom: Boolean,
    hazeState: HazeState,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    contentColor: Color = Theme.ScrollFab.contentColor,
) {
    AnimatedVisibility(
        visible = visible && (showScrollToTop || showScrollToBottom),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Theme.ScrollFab.spacing)
        ) {
            ScrollFabSlot(
                visible = showScrollToTop,
                onClick = onScrollToTop,
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll to top",
                hazeState = hazeState,
                contentColor = contentColor
            )

            ScrollFabSlot(
                visible = showScrollToBottom,
                onClick = onScrollToBottom,
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll to bottom",
                hazeState = hazeState,
                contentColor = contentColor
            )
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun ScrollFabSlot(
    visible: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    hazeState: HazeState,
    contentColor: Color,
) {
    Box(
        modifier = Modifier.size(Theme.ScrollFab.size),
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
                modifier = Modifier
                    .hazeGlass(
                        input = HazeInput.Sources(hazeState),
                        style = GlassStyle.regular.then {
                            backgroundColor(Theme.ScrollFab.backgroundColor)
                            tint(Theme.ScrollFab.tintColor)
                            shape(Theme.ScrollFab.shape)
                            whitePoint(Theme.ScrollFab.whitePoint)
                            specularIntensity(Theme.ScrollFab.specularIntensity)
                            ambientResponse(Theme.ScrollFab.ambientResponse)
                        }
                    )
                    .clip(Theme.ScrollFab.shape)
                    .border(
                        width = Theme.ScrollFab.borderWidth,
                        brush = Theme.ScrollFab.glassBorder,
                        shape = Theme.ScrollFab.shape
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription
                )
            }
        }
    }
}
