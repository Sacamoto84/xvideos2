package com.client.xvideos.common.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Верхний инсет выреза камеры (displayCutout).
 *
 * НЕ statusBars: приложение прячет системные бары (hide(systemBars) в MainActivity),
 * поэтому их инсет всегда 0 — реальный «верхний вырез» даёт только displayCutout.
 * На устройствах без выреза камеры (например, Samsung S7) возвращает 0.dp.
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

@Composable
fun getStatusBarInsetDp(): Dp {
    val density = LocalDensity.current
    return with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
}
