package com.client.xvideos.r.ui.fullscreen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.common.video.PlayerControls
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * [ScreenModel] для экрана полноэкранного воспроизведения RedGifs (TikTok-подобный вертикальный пейджер).
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
class ScreenRedFullScreenSM @Inject constructor(
    val downloadRed: DownloadRed,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val search: R_SearchExplorer,
) : ScreenModel {

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

/** Hilt-модуль привязки [ScreenRedFullScreenSM] в карту ScreenModel Voyager. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedFullScreen {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedFullScreenSM::class)
    abstract fun bindScreenRedFullScreenModel(screenModel: ScreenRedFullScreenSM): ScreenModel
}

/**
 * Валидирует и ограничивает временную метку точки A/B:
 * гарантирует неотрицательность, конечность и непревышение длительности видео.
 */
internal fun sanitizePointTime(time: Float, durationSec: Int): Float {
    if (!time.isFinite() || time < 0f) return 0f
    return if (durationSec > 0) time.coerceAtMost(durationSec.toFloat()) else time
}
