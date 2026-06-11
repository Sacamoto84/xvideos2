package com.client.xvideos.common.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * ```
 * val topInset = getTopInsetDp()
 *
 * Box(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .padding(top = topInset)
 * )
 * ```
 */
@Composable
fun getTopInsetDp(): Dp {
    val density = LocalDensity.current
    return with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
}