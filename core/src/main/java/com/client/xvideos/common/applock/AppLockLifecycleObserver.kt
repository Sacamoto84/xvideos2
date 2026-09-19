package com.client.xvideos.common.applock

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.client.xvideos.common.settings.Settings

/**
 * Отслеживает жизненный цикл всего приложения для таймера автоблокировки в фоне.
 *
 * Обрабатывает события [onStop] (все экраны свернуты) и [onStart] (приложение вернулось
 * на передний план) на уровне [ProcessLifecycleOwner].
 */
class AppLockLifecycleObserver(
    private val context: Context,
    private val elapsedRealtimeProvider: () -> Long = { android.os.SystemClock.elapsedRealtime() }
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        if (!AppLockRepository.isEnabled(context)) return

        AppLockSession.onAppBackgrounded(elapsedNow = elapsedRealtimeProvider())
        val timeoutSeconds = if (Settings.isInitialized) {
            Settings.app_lock_timeout_seconds.field.value
        } else {
            AppLockTimeout.DEFAULT.seconds
        }
        if (timeoutSeconds == AppLockTimeout.IMMEDIATELY.seconds) {
            AppLockSession.lock()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val isEnabled = AppLockRepository.isEnabled(context)
        val timeoutSeconds = if (Settings.isInitialized) {
            Settings.app_lock_timeout_seconds.field.value
        } else {
            AppLockTimeout.DEFAULT.seconds
        }
        AppLockSession.onAppForegrounded(
            timeoutSeconds = timeoutSeconds,
            isLockConfigured = isEnabled,
            elapsedNow = elapsedRealtimeProvider()
        )
    }

    companion object {
        private val installed = java.util.concurrent.atomic.AtomicBoolean(false)

        /**
         * Регистрирует наблюдатель в [ProcessLifecycleOwner].
         * Безопасно для повторного вызова (выполняется только один раз).
         */
        fun install(context: Context) {
            if (!installed.compareAndSet(false, true)) return
            val appContext = context.applicationContext
            ProcessLifecycleOwner.get().lifecycle.addObserver(AppLockLifecycleObserver(appContext))
        }
    }
}
