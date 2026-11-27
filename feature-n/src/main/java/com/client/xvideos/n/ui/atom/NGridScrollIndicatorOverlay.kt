package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.n.ui.model.NGridScrollIndicatorLayout

@Composable
internal fun BoxScope.NGridScrollIndicatorOverlay(
    layout: NGridScrollIndicatorLayout,
    hostTopInRoot: Int,
) {
    val height = with(LocalDensity.current) { layout.height.toDp() }
    Box(
        Modifier.align(Alignment.TopEnd)
            .offset { IntOffset(0, layout.topInRoot - hostTopInRoot) }
            .height(height)
            .width(2.dp)
    ) {
        key(layout.visibleRange) {
            VerticalScrollbar { layout.visibleRange.value }
        }
    }
}

@Preview
@Composable
private fun NGridScrollIndicatorOverlayPreview() {
    val range = remember { mutableStateOf(0.1f to 0.5f) }
    Box(Modifier.fillMaxSize()) {
        NGridScrollIndicatorOverlay(
            layout = NGridScrollIndicatorLayout(visibleRange = range, topInRoot = 0, height = 200),
            hostTopInRoot = 0,
        )
    }
}
