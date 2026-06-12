package com.client.xvideos.common.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Верхний инсет выреза камеры (displayCutout).
 *
 * НЕ statusBars: приложение прячет системные бары (hide(systemBars) в MainActivity),
 * поэтому их инсет всегда 0 — реальный «верхний вырез» даёт только displayCutout.
 *
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
        WindowInsets.displayCutout.getTop(this).toDp()
    }
}