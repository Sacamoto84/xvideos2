package com.client.xvideos.common.applock

import android.os.SystemClock

object AppLockSession {

    @Volatile
    private var unlocked = false

    @Volatile
    private var lastBackgroundElapsedMs: Long = 0L

    fun isUnlocked(): Boolean = unlocked

    fun unlock() {
        unlocked = true
        lastBackgroundElapsedMs = 0L
    }

    fun lock() {
        unlocked = false
        lastBackgroundElapsedMs = 0L
    }

    /**
     * Фиксирует момент ухода приложения в фон по монотонным часам.
     */
    fun onAppBackgrounded(elapsedNow: Long = SystemClock.elapsedRealtime()) {
        lastBackgroundElapsedMs = elapsedNow
    }

    /**
     * Проверяет длительность нахождения в фоне и при необходимости блокирует сессию.
     *
     * @param timeoutSeconds таймаут в секундах из настроек.
     * @param isLockConfigured включена ли блокировка в настройках и задан ли пароль.
     * @param elapsedNow текущее монотонное время (по умолчанию [SystemClock.elapsedRealtime]).
     * @return true, если сессия была заблокирована по таймауту.
     */
    fun onAppForegrounded(
        timeoutSeconds: Int,
        isLockConfigured: Boolean,
        elapsedNow: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        if (!isLockConfigured || !unlocked || lastBackgroundElapsedMs == 0L) {
            lastBackgroundElapsedMs = 0L
            return false
        }

        val timeout = AppLockTimeout.fromSeconds(timeoutSeconds)
        if (timeout == AppLockTimeout.NEVER) {
            lastBackgroundElapsedMs = 0L
            return false
        }

        val elapsed = elapsedNow - lastBackgroundElapsedMs
        val shouldLock = elapsed >= timeout.durationMillis
        if (shouldLock) {
            lock()
            return true
        }
        lastBackgroundElapsedMs = 0L
        return false
    }

    fun lastBackgroundTime(): Long = lastBackgroundElapsedMs
}
