package com.client.xvideos

import android.app.Activity
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Оформление окна, общее для MainActivity и LockActivity: экран замка обязан
// выглядеть продолжением приложения, а не отдельным окном с другими полями.

/** Содержимое заходит под вырез, у панели навигации нет системной подложки. */
internal fun Window.applyAppWindowStyle() {
    isNavigationBarContrastEnforced = false
    attributes = attributes?.apply {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}

/** Показывает или прячет статус-бар; спрятанный на время возвращается свайпом. */
internal fun Window.setStatusBarVisible(visible: Boolean) {
    val controller = WindowCompat.getInsetsController(this, decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (visible) {
        controller.show(WindowInsetsCompat.Type.statusBars())
    } else {
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }
}

/** Есть ли вырез сверху — до первой композиции, чтобы бар не мигал при запуске. */
@Suppress("DEPRECATION")
internal fun Activity.hasTopCutout(): Boolean {
    val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.cutout
    } else {
        windowManager.defaultDisplay.cutout
    }
    return (cutout?.safeInsetTop ?: 0) > 0
}
