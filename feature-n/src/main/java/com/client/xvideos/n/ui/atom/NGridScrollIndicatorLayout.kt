package com.client.xvideos.n.ui.atom

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf

internal data class NGridScrollIndicatorLayout(
    val visibleRange: State<Pair<Float, Float>>,
    val topInRoot: Int,
    val height: Int,
)

internal val LocalNGridScrollIndicator =
    staticCompositionLocalOf<MutableState<NGridScrollIndicatorLayout?>?> { null }
