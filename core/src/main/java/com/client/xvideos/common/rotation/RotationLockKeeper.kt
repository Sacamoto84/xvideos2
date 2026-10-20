package com.client.xvideos.common.rotation

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Возвращает системную блокировку поворота, если она была включена, когда Activity
 * приложения стала верхней, а к уходу оказалась снята.
 *
 * Сигнал — `Activity.onTopResumedActivityChanged`, его передают все Activity
 * приложения через [onTopResumedActivityChanged]. Жизненного цикла процесса мало:
 * в «Недавних» с живой плиткой Activity остаётся `RESUMED`, `ON_STOP` процесса не
 * приходит, а смахивание карточки убивает процесс (проход 200, T1). Верхней Activity
 * перестаёт быть и при «Домой», «Назад», «Недавних», переходе в другое приложение
 * или системное окно-Activity; шторка уведомлений её не меняет — снять блокировку
 * из шторки внутри приложения можно.
 *
 * Без права «Изменение системных настроек» ([RotationSettings.canWrite]) ничего не
 * меняет.
 */
@Singleton
class RotationLockKeeper @Inject constructor(private val settings: RotationSettings) {

    /** Положение блокировки, когда Activity стала верхней; `null` — блокировки не было. */
    private var lockedAtTop: Int? = null

    /** Activity приложения стала верхней: запомнить блокировку. */
    fun onTop() {
        lockedAtTop = if (settings.isAutoRotateOn()) null else settings.lockedRotation()
    }

    /** Activity приложения перестала быть верхней: вернуть блокировку, если её сняли. */
    fun onLeftTop() {
        val rotation = lockedAtTop ?: return
        lockedAtTop = null
        if (settings.isAutoRotateOn() && settings.canWrite()) {
            settings.lock(rotation)
        }
    }

    companion object {

        /**
         * Вызывается из `onTopResumedActivityChanged` каждой Activity приложения —
         * в том числе `LockActivity`, которой Hilt ничего не внедряет. Система сначала
         * сообщает `false` прежней верхней, потом `true` новой, так что переход между
         * своими Activity — это возврат и новое запоминание.
         */
        fun onTopResumedActivityChanged(context: Context, isTopResumedActivity: Boolean) {
            val keeper = EntryPointAccessors
                .fromApplication(context.applicationContext, RotationLockEntryPoint::class.java)
                .rotationLockKeeper()
            if (isTopResumedActivity) keeper.onTop() else keeper.onLeftTop()
        }
    }
}

/** Доступ к [RotationLockKeeper] из Activity вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RotationLockEntryPoint {
    fun rotationLockKeeper(): RotationLockKeeper
}
