package com.client.xvideos.common.applock

/**
 * Результат проверки введённого кода на экране замка [AppLockRepository.verifyUnlockCode].
 */
sealed interface AppLockUnlockResult {

    /** Введён верный код доступа: вход разрешён. */
    data object AccessGranted : AppLockUnlockResult

    /** Введён код уничтожения: необходимо стереть данные и перезапустить приложение. */
    data object DuressTriggered : AppLockUnlockResult

    /** Введён неверный код. */
    data object Wrong : AppLockUnlockResult
}
