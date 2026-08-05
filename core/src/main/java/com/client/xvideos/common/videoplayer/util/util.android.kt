package com.client.xvideos.common.videoplayer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Base64
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.client.xvideos.common.videoplayer.host.DrmConfig
import java.nio.charset.StandardCharsets

@SuppressLint("DefaultLocale")
fun formatMinSec(value: Int): String {
    return if (value == 0) {
        "00:00"
    } else {
        // Calculate hours, minutes, and seconds
        val hours = value / 3600
        val minutes = (value % 3600) / 60
        val seconds = value % 60

        // Format the output string
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}


@Composable
fun LandscapeOrientation(
    enableFullEdgeToEdge: Boolean,
    isLandscape: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val window = activity?.window
    val windowInsetsController = window?.let {
        WindowCompat.getInsetsController(it, it.decorView)
    }

    windowInsetsController?.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    fun reset() {
        // Возврат к глобальной конфигурации MainActivity: SHORT_EDGES, edge-to-edge
        // не выключаем (setDecorFitsSystemWindows(true) ломал прозрачные бары).
        if (enableFullEdgeToEdge) {
            window?.attributes = window.attributes?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        windowInsetsController?.show(WindowInsetsCompat.Type.navigationBars())
        // Страховка: статус-бар обязан остаться скрытым
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            if (enableFullEdgeToEdge) {
                window?.attributes = window.attributes?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            windowInsetsController?.hide(WindowInsetsCompat.Type.navigationBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            reset()
        }
    }

    DisposableEffect(isLandscape) {
        onDispose {
            reset()
        }
    }
    content()
}

internal object VideoUtils {
    private fun encodeBase64(input: String): String {
        val bytes = input.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun createDrmJson(drmConfig: DrmConfig): ByteArray {
        val key = encodeBase64(drmConfig.key)
        val keyId = encodeBase64(drmConfig.keyId)
        val json = """{"keys":[{"kty":"oct","k":"$key","kid":"$keyId"}],"type":"temporary"}"""
        return json.toByteArray(StandardCharsets.UTF_8)
    }
}
