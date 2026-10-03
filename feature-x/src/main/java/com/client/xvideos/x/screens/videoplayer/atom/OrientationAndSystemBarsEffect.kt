package com.client.xvideos.x.screens.videoplayer.atom

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.client.xvideos.common.ui.statusbar.HideStatusBarEffect
import com.client.xvideos.common.util.findActivity

/**
 * Управляет ориентацией экрана (альбомная/портретная) и нижней панелью навигации.
 * В полноэкранном режиме прячет панель навигации и подаёт заявку на скрытие
 * статус-бара; при выходе или закрытии экрана возвращает стандартные настройки.
 */
@Composable
fun OrientationAndSystemBarsEffect(isFullScreen: Boolean) {
    // Развёрнутый плеер — полноэкранный экран: статус-бар скрыт, пока он развёрнут.
    if (isFullScreen) {
        HideStatusBarEffect()
    }

    val context = LocalContext.current

    // Альбомная ориентация + скрытие панели навигации на время полноэкранного режима
    DisposableEffect(isFullScreen) {
        val activity = context.findActivity()
        val window = activity?.window
        val prevOrientation = activity?.requestedOrientation
        if (isFullScreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            window?.let {
                val controller = WindowCompat.getInsetsController(it, it.decorView)
                // Скрытые бары временно появляются по свайпу от края экрана
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            window?.let {
                val controller = WindowCompat.getInsetsController(it, it.decorView)
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
        onDispose {
            if (isFullScreen) {
                activity?.requestedOrientation =
                    prevOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window?.let {
                    val controller = WindowCompat.getInsetsController(it, it.decorView)
                    controller.show(WindowInsetsCompat.Type.navigationBars())
                }
            }
        }
    }

    // При полном уходе с экрана гарантированно возвращаем портретную ориентацию и восстанавливаем навигацию
    DisposableEffect(Unit) {
        onDispose {
            val activity = context.findActivity()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let {
                val controller = WindowCompat.getInsetsController(it, it.decorView)
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
}

@Preview
@Composable
private fun OrientationAndSystemBarsEffectPreview() {
    OrientationAndSystemBarsEffect(isFullScreen = false)
}
