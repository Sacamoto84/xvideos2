package com.client.xvideos.common.applock

import androidx.compose.runtime.compositionLocalOf

/**
 * Флаг активности блокировки приложения или приватного режима экрана.
 *
 * Когда `true`, все воспроизводящие компоненты ([CMPPlayer2], [RedPooledVideoPlayer])
 * немедленно приостанавливают воспроизведение, сбрасывают громкость до 0
 * и отключают удержание экрана включённым ([KeepScreenOn]).
 */
val LocalAppLockActive = compositionLocalOf { false }
