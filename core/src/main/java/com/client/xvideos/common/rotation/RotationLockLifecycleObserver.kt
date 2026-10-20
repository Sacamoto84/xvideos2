package com.client.xvideos.common.rotation

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Возвращает системную блокировку поворота, если она была включена, когда приложение
 * вышло на передний план, а к уходу в фон оказалась снята.
 *
 * Работает на уровне [ProcessLifecycleOwner]: [onStart] — приложение открыто,
 * [onStop] — все его экраны свёрнуты или закрыты. Без права «Изменение системных
 * настроек» ([RotationSettings.canWrite]) ничего не меняет.
 */
class RotationLockLifecycleObserver(
    private val settings: RotationSettings,
) : DefaultLifecycleObserver {

    /** Положение блокировки при выходе на передний план; `null` — блокировки не было. */
    private var lockedAtStart: Int? = null

    override fun onStart(owner: LifecycleOwner) {
        lockedAtStart = if (settings.isAutoRotateOn()) null else settings.lockedRotation()
    }

    override fun onStop(owner: LifecycleOwner) {
        val rotation = lockedAtStart ?: return
        lockedAtStart = null
        if (settings.isAutoRotateOn() && settings.canWrite()) {
            settings.lock(rotation)
        }
    }

    companion object {
        private val installed = java.util.concurrent.atomic.AtomicBoolean(false)

        /**
         * Регистрирует наблюдатель в [ProcessLifecycleOwner].
         * Безопасно для повторного вызова (выполняется только один раз).
         */
        fun install(context: Context) {
            if (!installed.compareAndSet(false, true)) return
            val settings = SystemRotationSettings(context.applicationContext)
            ProcessLifecycleOwner.get().lifecycle.addObserver(RotationLockLifecycleObserver(settings))
        }
    }
}
