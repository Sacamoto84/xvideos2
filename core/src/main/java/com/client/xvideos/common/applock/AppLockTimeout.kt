package com.client.xvideos.common.applock

/**
 * Варианты времени неактивности приложения в фоне до автоблокировки.
 *
 * @property seconds Длительность в секундах (0 — сразу, -1 — никогда).
 * @property displayName Локализованное название для отображения в интерфейсе настроек.
 */
enum class AppLockTimeout(
    val seconds: Int,
    val displayName: String
) {
    IMMEDIATELY(0, "Сразу"),
    SECONDS_30(30, "30 секунд"),
    MINUTES_1(60, "1 минута"),
    MINUTES_5(300, "5 минут"),
    NEVER(-1, "Никогда");

    val durationMillis: Long
        get() = if (seconds <= 0) 0L else seconds * 1000L

    val isImmediately: Boolean get() = this == IMMEDIATELY
    val isNever: Boolean get() = this == NEVER
    val isAutoLocking: Boolean get() = this != NEVER

    companion object {
        val DEFAULT = MINUTES_1

        fun fromSecondsOrNull(seconds: Int?): AppLockTimeout? =
            if (seconds != null) entries.firstOrNull { it.seconds == seconds } else null

        fun fromSeconds(seconds: Int?, default: AppLockTimeout = DEFAULT): AppLockTimeout =
            fromSecondsOrNull(seconds) ?: default
    }
}
