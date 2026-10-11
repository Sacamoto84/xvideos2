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
    fun onAppBackgrounded(elapsedNow: Long = currentElapsed()) {
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
        elapsedNow: Long = currentElapsed()
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

    /**
     * Проверяет, активна ли разблокированная сессия с учётом таймаута нахождения в фоне.
     * Если приложение находится в фоне дольше установленного таймаута [timeoutSeconds],
     * сессия автоматически блокируется.
     *
     * Используется фоновыми сервисами (например, локальным веб-сервером) для моментального
     * закрытия доступа к данным при блокировке устройства.
     */
    fun checkSessionValid(
        timeoutSeconds: Int,
        isLockConfigured: Boolean = true,
        elapsedNow: Long = currentElapsed()
    ): Boolean {
        if (!isLockConfigured) return true
        if (!unlocked) return false
        if (lastBackgroundElapsedMs == 0L) return true

        val timeout = AppLockTimeout.fromSeconds(timeoutSeconds)
        if (timeout == AppLockTimeout.NEVER) return true

        val elapsed = elapsedNow - lastBackgroundElapsedMs
        if (elapsed >= timeout.durationMillis) {
            lock()
            return false
        }
        return true
    }

    fun lastBackgroundTime(): Long = lastBackgroundElapsedMs

    private fun currentElapsed(): Long {
        return try {
            SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }
}
