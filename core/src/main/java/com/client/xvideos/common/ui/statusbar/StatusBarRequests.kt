package com.client.xvideos.common.ui.statusbar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Виден ли системный статус-бар.
 *
 * @param hasTopCutout У устройства есть вырез камеры сверху: бар занимает его полосу.
 * @param hideRequests Сколько экранов в композиции попросили скрыть бар.
 */
fun shouldShowStatusBar(hasTopCutout: Boolean, hideRequests: Int): Boolean =
    hasTopCutout && hideRequests == 0

/**
 * Заявки полноэкранных экранов на скрытие статус-бара.
 *
 * Счётчик, а не флаг: при переходе между двумя полноэкранными экранами новый
 * входит в композицию раньше, чем старый её покидает.
 *
 * Экземпляр создаёт корень приложения и раздаёт через [LocalStatusBarRequests].
 */
@Stable
class StatusBarRequests {

    /** Число живых заявок. Наблюдаемое: корень пересчитывает видимость бара. */
    var hideRequests: Int by mutableIntStateOf(0)
        private set

    fun acquireHide() {
        hideRequests += 1
    }

    fun releaseHide() {
        hideRequests = (hideRequests - 1).coerceAtLeast(0)
    }
}

/**
 * Заявки на скрытие статус-бара от корня приложения. Вне корня (превью) —
 * отдельный экземпляр, на который никто не смотрит.
 */
val LocalStatusBarRequests = staticCompositionLocalOf { StatusBarRequests() }
