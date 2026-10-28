package com.client.xvideos.duress

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.client.xvideos.SplashActivity
import com.client.xvideos.common.duress.DuressTokenStore
import com.client.xvideos.common.duress.DuressWipeIsolation
import timber.log.Timber

/**
 * Координатор срабатывания кода уничтожения:
 * 1. Изолирует данные приложения через [DuressWipeIsolation.isolate].
 * 2. Снимает все уведомления приложения.
 * 3. Генерирует одноразовый криптографический токен открытия сессии.
 * 4. Запускает [RestartActivity] в изолированном процессе `:restart` и завершает текущий процесс.
 */
object DuressLauncher {

    fun execute(activity: Activity): Boolean {
        // 1. Изоляция данных
        val isolated = DuressWipeIsolation.isolate(activity)
        if (!isolated) {
            Timber.e("DuressLauncher: изоляция данных завершилась с ошибкой, отмена перезапуска")
            return false
        }

        // 2. Снятие всех уведомлений
        runCatching {
            val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancelAll()
        }.onFailure {
            Timber.w(it, "DuressLauncher: не удалось снять уведомления")
        }

        // 3. Создание одноразового токена
        val token = DuressTokenStore.createOneTimeToken(activity)

        // 4. Подготовка и запуск перезапуска
        val splashIntent = Intent(activity, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(DuressTokenStore.EXTRA_DURESS_TOKEN, token)
        }

        val restartIntent = Intent(activity, RestartActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(RestartActivity.EXTRA_OLD_PID, Process.myPid())
            putExtra(RestartActivity.EXTRA_TARGET_INTENT, splashIntent)
        }

        activity.startActivity(restartIntent)
        activity.finish()
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(0, 0)
        Process.killProcess(Process.myPid())
        return true
    }
}
