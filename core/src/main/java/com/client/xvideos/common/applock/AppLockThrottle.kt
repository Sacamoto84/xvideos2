package com.client.xvideos.common.applock

/**
 * Арифметика блокировки ввода код-доступа: чистые функции, без `Context`.
 *
 * Вынесено из [AppLockRepository] по двум причинам. Первая — тестируемость:
 * репозиторий тянет `SharedPreferences` и `android.util.Base64`, которых в
 * JVM-тестах нет, а проверять надо именно арифметику. Вторая — сам дефект:
 * раньше срок считался только по `System.currentTimeMillis()`, и перевод
 * системного времени вперёд снимал блокировку мгновенно, без root. При
 * четырёхзначном коде backoff — единственная реальная защита от перебора.
 *
 * Поэтому срок хранится дважды: настенными часами (переживают перезагрузку) и
 * монотонными (`SystemClock.elapsedRealtime`, не поддаются переводу времени).
 * Блокировка держится, пока не истёк хотя бы один из них.
 */
object AppLockThrottle {

    /** Сколько попыток без задержки до начала блокировки. */
    const val FREE_ATTEMPTS = 4

    /** Базовая длительность блокировки; далее удваивается на каждую ошибку. */
    const val BASE_LOCKOUT_MS = 30_000L
    const val MAX_LOCKOUT_MS = 30 * 60_000L // 30 минут
    private const val MAX_BACKOFF_SHIFT = 16

    /**
     * @param attempts общее число неудачных попыток, включая последнюю.
     * @param lockoutUntilWall срок по настенным часам (epoch millis), 0 — блокировки нет.
     * @param lockoutUntilElapsed срок по монотонным часам, 0 — блокировки нет.
     */
    data class State(
        val attempts: Int,
        val lockoutUntilWall: Long,
        val lockoutUntilElapsed: Long,
    )

    /**
     * Регистрирует неудачную попытку.
     *
     * @param attempts сколько ошибок было ДО этой.
     */
    fun onFailedAttempt(attempts: Int, wallNow: Long, elapsedNow: Long): State {
        val total = attempts + 1
        if (total <= FREE_ATTEMPTS) {
            return State(attempts = total, lockoutUntilWall = 0L, lockoutUntilElapsed = 0L)
        }
        val shift = (total - FREE_ATTEMPTS - 1).coerceIn(0, MAX_BACKOFF_SHIFT)
        val duration = (BASE_LOCKOUT_MS shl shift).coerceAtMost(MAX_LOCKOUT_MS)
        return State(
            attempts = total,
            lockoutUntilWall = wallNow + duration,
            lockoutUntilElapsed = elapsedNow + duration,
        )
    }

    /**
     * Сколько миллисекунд осталось до конца блокировки (0 — ввод разрешён).
     *
     * Берётся максимум из двух остатков. Монотонный остаток отбрасывается,
     * если он больше максимально возможной блокировки: так выглядит
     * перезагрузка, после которой `elapsedRealtime` начинается заново и
     * сохранённый срок оказывается «в будущем» навсегда.
     */
    fun remainingMillis(state: State, wallNow: Long, elapsedNow: Long): Long {
        val byWall = (state.lockoutUntilWall - wallNow).coerceAtLeast(0L)
        val byElapsedRaw = (state.lockoutUntilElapsed - elapsedNow).coerceAtLeast(0L)
        val byElapsed = if (byElapsedRaw > MAX_LOCKOUT_MS) 0L else byElapsedRaw
        return maxOf(byWall, byElapsed)
    }
}
