package com.client.xvideos.common.ui.atom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

/**
 * Плавающие кнопки быстрой прокрутки ("Вверх" и "Вниз").
 * Поддерживают 3 режима отображения ([ScrollButtonEffect]):
 * - [ScrollButtonEffect.FLAT]: Сплошной цвет с легкой прозрачностью (0% нагрузки на GPU);
 * - [ScrollButtonEffect.BLUR]: Матовый блюр фона (HazeBlurStyle);
 * - [ScrollButtonEffect.GLASS]: Оптическое стекло с рефракцией и бликами (hazeGlass).
 *
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
    effect: ScrollButtonEffect? = null,
) {
    val resolvedEffect = effect ?: run {
        val effectName = Settings.scroll_buttons_effect.field.collectAsStateWithLifecycle().value
        remember(effectName) { ScrollButtonEffect.fromNameOrDefault(effectName) }
    }

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
                contentColor = contentColor,
                effect = resolvedEffect
            )

            ScrollFabSlot(
                visible = showScrollToBottom,
                onClick = onScrollToBottom,
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll to bottom",
                hazeState = hazeState,
                contentColor = contentColor,
                effect = resolvedEffect
            )
        }
    }
}

@Composable
private fun ScrollFabSlot(
    visible: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    hazeState: HazeState,
    contentColor: Color,
    effect: ScrollButtonEffect,
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
                    .scrollFabVisualEffect(effect = effect, hazeState = hazeState)
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

@OptIn(ExperimentalHazeApi::class)
private fun Modifier.scrollFabVisualEffect(
    effect: ScrollButtonEffect,
    hazeState: HazeState
): Modifier = when (effect) {
    ScrollButtonEffect.FLAT -> this.background(
        color = Theme.ScrollFab.backgroundColor,
        shape = Theme.ScrollFab.shape
    )
    ScrollButtonEffect.BLUR -> this.hazeBlur(
        input = HazeInput.Sources(hazeState),
        style = HazeBlurStyle.then {
            backgroundColor(Theme.ScrollFab.backgroundColor)
            blurRadius(Theme.ScrollFab.blurRadius)
            noiseFactor(Theme.ScrollFab.noiseFactor)
        }
    )
    ScrollButtonEffect.GLASS -> this.hazeGlass(
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
}
