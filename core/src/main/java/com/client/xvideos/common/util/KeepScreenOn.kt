package com.client.xvideos.common.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

/**
 * Не даёт экрану гаснуть, пока [active].
 *
 * Зовётся плеерами на время воспроизведения. Раньше экран держал включённым
 * безусловный вызов в корне приложения: он не гас нигде — ни в списках, ни в
 * настройках, ни на экране замка, — телефон не засыпал и не блокировался, а
 * замок приложения, который срабатывает после ухода в фон, не срабатывал.
 *
 * @param active Держать экран включённым: идёт воспроизведение.
 */
@Composable
fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        if (active) {
            KeepScreenOnCounter.acquire(view)
            onDispose { KeepScreenOnCounter.release(view) }
        } else {
            onDispose { }
        }
    }
}

/**
 * Счётчик владельцев флага на одном host-view: `keepScreenOn` — свойство view,
 * а не плеера, и плееров на экране может быть несколько (списки с
 * `UrlVideoLite`): снимать флаг можно, лишь когда замолчал последний.
 * Только главный поток — Compose-эффекты выполняются на нём.
 */
private object KeepScreenOnCounter {

    // WeakHashMap, а не mutableMapOf: карта переживает свои View (счётчик —
    // process-wide object), и непарный release превратил бы её в утечку всей
    // Compose-иерархии. Слабый ключ делает такую утечку невозможной.
    private val counts = WeakHashMap<View, Int>()

    fun acquire(view: View) {
        val next = (counts[view] ?: 0) + 1
        counts[view] = next
        view.keepScreenOn = true
    }

    fun release(view: View) {
        val next = (counts[view] ?: 1) - 1
        if (next <= 0) {
            counts.remove(view)
            view.keepScreenOn = false
        } else {
            counts[view] = next
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
