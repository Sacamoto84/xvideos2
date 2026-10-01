package com.client.xvideos.x.screens.channel.model

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Состояние схлопывающейся шапки канала. Смещение шапки переживает пересоздание
 * экрана, высота шапки измеряется заново.
 *
 * @param currentGrid Сетка текущей страницы пейджера; берётся последняя переданная.
 */
@Composable
fun rememberChannelHeaderCollapseState(currentGrid: () -> LazyGridState): ChannelHeaderCollapseState {
    val offsetState = rememberSaveable { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val latestGrid by rememberUpdatedState(currentGrid)
    return remember(offsetState, scope) {
        ChannelHeaderCollapseState(offsetState, scope) { latestGrid() }
    }
}
