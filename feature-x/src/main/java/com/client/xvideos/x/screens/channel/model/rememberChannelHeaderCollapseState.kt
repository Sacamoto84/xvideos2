package com.client.xvideos.x.screens.channel.model

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState

/**
 * Состояние схлопывающейся шапки канала. Смещение шапки хранит ScreenModel, высота
 * шапки измеряется заново.
 *
 * @param offsetState Смещение шапки из ScreenModel: корневой навигатор не хранит
 *   состояние экрана, с которого ушли, и после ролика шапка разворачивалась над
 *   прокрученной сеткой.
 * @param currentGrid Сетка текущей страницы пейджера, если она на экране; берётся последняя переданная.
 */
@Composable
fun rememberChannelHeaderCollapseState(
    offsetState: MutableFloatState,
    currentGrid: () -> LazyGridState?,
): ChannelHeaderCollapseState {
    val scope = rememberCoroutineScope()
    val decayFling = ScrollableDefaults.flingBehavior()
    val latestGrid by rememberUpdatedState(currentGrid)
    return remember(offsetState, scope, decayFling) {
        ChannelHeaderCollapseState(offsetState, scope, decayFling) { latestGrid() }
    }
}
