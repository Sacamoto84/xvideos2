package com.client.xvideos.common.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings

/**
 * Верхний инсет выреза камеры (displayCutout).
 *
 * Учитывает пользовательскую настройку [Settings.useCutoutPadding]: если
 * тумблер выключен, возвращает 0.dp.
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
    val enabled = if (Settings.isInitialized) {
        Settings.useCutoutPadding.field.collectAsStateWithLifecycle().value
    } else {
        true
    }
    if (!enabled) return 0.dp

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
