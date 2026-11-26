package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.scrollFabSurface
import dev.chrisbanes.haze.HazeState

@Composable
internal fun FastScrollHandle(
    modifier: Modifier,
    effect: ScrollButtonEffect,
    hazeState: HazeState,
) {
    Box(modifier, contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier
                .padding(end = 2.dp)
                .size(width = 30.dp, height = 38.dp)
                .scrollFabSurface(effect = effect, hazeState = hazeState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val tint = Theme.ScrollFab.contentColor
            Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(16.dp), tint)
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp), tint)
        }
    }
}

@Preview
@Composable
private fun FastScrollHandlePreview() {
    FastScrollHandle(
        modifier = Modifier.size(48.dp),
        effect = ScrollButtonEffect.FLAT,
        hazeState = HazeState(),
    )
}
