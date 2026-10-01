package com.client.xvideos.r.ui.fullscreen.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.r.common.video.PlayerControls
import com.client.xvideos.r.ui.fullscreen.sanitizePointTime

/**
 * Состояние плеера полноэкранного режима R (TikTok-подобный вертикальный пейджер).
 *
 * Живёт в [com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreenSM], но молекулы
 * получают только его, а не ScreenModel целиком.
 *
 * Управляет состоянием:
 * - Воспроизведением/паузой [play];
 * - Отключением звука [mute];
 * - Автоповоротом экрана [autoRotate];
 * - Скоростью воспроизведения [speed];
 * - Режимом зацикливания отрезка A-B ([enableAB], [timeA], [timeB]);
 * - Делегированием команд текущему активному видеоплееру [currentPlayerControls].
 */
@Stable
class RedFullScreenPlayerState {

    /** Флаг активного воспроизведения видео. */
    var play by mutableStateOf(true)
    /** Флаг заглушения аудиодорожки. */
    var mute by mutableStateOf(true)
    /** Флаг автоматического поворота экрана в горизонтальную ориентацию. */
    var autoRotate by mutableStateOf(false)
    /** Текущая скорость воспроизведения плеера. */
    var speed by mutableStateOf(PlayerSpeed.DEFAULT)

    /** Флаг активности режима циклического повтора отрезка A-B. */
    var enableAB by mutableStateOf(false)
    /** Временная метка начала петли A (в секундах). */
    var timeA by mutableFloatStateOf(3f)
    /** Временная метка конца петли B (в секундах). */
    var timeB by mutableFloatStateOf(6f)

    /** Ссылка на контролы управления текущим видимым видеоплеером. */
    var currentPlayerControls by mutableStateOf<PlayerControls?>(null)

    /** Текущая позиция воспроизведения в секундах. */
    var currentPlayerTime by mutableFloatStateOf(0f)
    /** Общая длительность текущего ролика в секундах. */
    var currentPlayerDuration by mutableIntStateOf(0)

    /**
     * Фиксирует точку A петли повтора по текущему положению воспроизведения.
     */
    fun setTimeA() {
        timeA = sanitizePointTime(currentPlayerTime, currentPlayerDuration)
        if (enableAB && timeB <= timeA) {
            enableAB = false
        }
    }

    /**
     * Фиксирует точку B петли повтора по текущему положению воспроизведения.
     */
    fun setTimeB() {
        timeB = sanitizePointTime(currentPlayerTime, currentPlayerDuration)
        if (enableAB && timeB <= timeA) {
            enableAB = false
        }
    }

    /**
     * Переключает режим циклического повтора отрезка A-B с валидацией границ.
     */
    fun toggleAB() {
        if (!enableAB && timeB <= timeA) {
            SnackBar.warning("Точка B должна быть больше точки A")
        } else {
            enableAB = !enableAB
        }
    }

    /**
     * Переключает состояние воспроизведения (Play/Pause).
     */
    fun togglePlay() {
        play = !play
        if (play) {
            currentPlayerControls?.play()
        } else {
            currentPlayerControls?.pause()
        }
    }

    /** Перемотка назад на [seconds] секунд. */
    fun rewind(seconds: Float = 1f) {
        currentPlayerControls?.rewind(seconds)
    }

    /** Перемотка вперед на [seconds] секунд. */
    fun forward(seconds: Float = 1f) {
        currentPlayerControls?.forward(seconds)
    }

    /** Переключение звука (Mute/Unmute). */
    fun toggleMute() {
        mute = !mute
    }

    /** Сброс скорости воспроизведения на стандартную 1.0x. */
    fun resetSpeed() {
        speed = PlayerSpeed.DEFAULT
    }
}
