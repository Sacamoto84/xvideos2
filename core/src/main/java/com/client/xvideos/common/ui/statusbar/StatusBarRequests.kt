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
 * Заявки экранов к статус-бару: полноэкранные просят скрыть бар, тёмные —
 * убрать подложку под ним.
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

    /**
     * Число живых заявок на бар без подложки. Их подают экраны, у которых под
     * баром и так почти чёрный фон: подложка цвета общей темы на нём светится серым.
     */
    var noScrimRequests: Int by mutableIntStateOf(0)
        private set

    fun acquireNoScrim() {
        noScrimRequests += 1
    }

    fun releaseNoScrim() {
        noScrimRequests = (noScrimRequests - 1).coerceAtLeast(0)
    }
}

/**
 * Заявки к статус-бару от корня приложения. Вне корня (превью) —
 * отдельный экземпляр, на который никто не смотрит.
 */
val LocalStatusBarRequests = staticCompositionLocalOf { StatusBarRequests() }
