package com.client.xvideos.common.applock

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/**
 * Флаг активности блокировки приложения или приватного режима экрана.
 *
 * Когда `true`, все воспроизводящие компоненты ([CMPPlayer2], [RedPooledVideoPlayer])
 * немедленно приостанавливают воспроизведение, сбрасывают громкость до 0
 * и отключают удержание экрана включённым ([KeepScreenOn]).
 *
 * Отдаётся объектом состояния, а не значением. В композиции его читают через
 * `.value`, и смена перекомпоновывает плееры как прежде. Обработчики жизненного
 * цикла (`ON_RESUME`, `ON_START`) читают `.value` в момент события: `MainActivity`
 * закрывает сеанс в `onStart`, а значение композиции доходит до плеера только со
 * следующим кадром — вернувшийся под замок плеер успевал запуститься (проход 201, S1).
 */
val LocalAppLockActive = compositionLocalOf<State<Boolean>> { mutableStateOf(false) }
