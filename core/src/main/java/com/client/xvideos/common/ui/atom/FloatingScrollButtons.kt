package com.client.xvideos.common.ui.atom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import dev.chrisbanes.haze.HazeState

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
    val effectName by Settings.scroll_buttons_effect.field.collectAsStateWithLifecycle()
    val resolvedEffect = effect ?: remember(effectName) { ScrollButtonEffect.fromNameOrDefault(effectName) }

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
                contentDescription = "Прокрутить вверх",
                hazeState = hazeState,
                contentColor = contentColor,
                effect = resolvedEffect
            )

            ScrollFabSlot(
                visible = showScrollToBottom,
                onClick = onScrollToBottom,
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Прокрутить вниз",
                hazeState = hazeState,
                contentColor = contentColor,
                effect = resolvedEffect
            )
        }
    }
}

@Preview
@Composable
private fun FloatingScrollButtonsPreview() {
    FloatingScrollButtons(
        showScrollToTop = true,
        showScrollToBottom = true,
        hazeState = remember { HazeState() },
        onScrollToTop = {},
        onScrollToBottom = {}
    )
}
