package com.client.xvideos.common.applock

/** Итог [AppLockRepository.verifyPasswordThrottled]. */
sealed interface AppLockCheckResult {

    /** Код верный, счётчик попыток сброшен. */
    data object Success : AppLockCheckResult

    /**
     * Код не подошёл, попытка засчитана.
     *
     * @param lockoutRemainingMs больше нуля, если эта ошибка включила блокировку ввода.
     */
    data class Wrong(val lockoutRemainingMs: Long) : AppLockCheckResult

    /** Ввод заблокирован после серии ошибок: код не проверялся, попытка не засчитана. */
    data class LockedOut(val remainingMs: Long) : AppLockCheckResult
}
