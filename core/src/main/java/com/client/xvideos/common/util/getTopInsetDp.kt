package com.client.xvideos.common.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Верхний отступ экрана: большее из выреза камеры (displayCutout) и статус-бара.
 *
 * Статус-бар виден только на устройствах с вырезом и не на полноэкранных экранах
 * (см. `StatusBarRequests`); когда он скрыт, его инсет равен 0 и остаётся вырез.
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
        maxOf(
            WindowInsets.displayCutout.getTop(this),
            WindowInsets.statusBars.getTop(this),
        ).toDp()
    }
}

@Composable
fun getStatusBarInsetDp(): Dp {
    val density = LocalDensity.current
    return with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
}
