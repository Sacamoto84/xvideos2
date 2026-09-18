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
    private val context: Context
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        if (!AppLockRepository.isEnabled(context)) return

        AppLockSession.onAppBackgrounded()
        val timeoutSeconds = Settings.app_lock_timeout_seconds.field.value
        if (timeoutSeconds == AppLockTimeout.IMMEDIATELY.seconds) {
            AppLockSession.lock()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val isEnabled = AppLockRepository.isEnabled(context)
        val timeoutSeconds = Settings.app_lock_timeout_seconds.field.value
        AppLockSession.onAppForegrounded(
            timeoutSeconds = timeoutSeconds,
            isLockConfigured = isEnabled
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
