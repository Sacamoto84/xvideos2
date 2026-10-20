package com.client.xvideos.common.rotation

/**
 * Системная блокировка поворота экрана, которую читает и возвращает
 * [RotationLockKeeper]. Интерфейс отделяет логику от `android.provider.Settings`,
 * чтобы её можно было проверить юнит-тестом.
 */
interface RotationSettings {

    /** Автоповорот включён: системная блокировка поворота снята. */
    fun isAutoRotateOn(): Boolean

    /** Положение экрана, в котором держится блокировка (`Surface.ROTATION_*`). */
    fun lockedRotation(): Int

    /** Пользователь дал приложению право «Изменение системных настроек». */
    fun canWrite(): Boolean

    /** Включает блокировку поворота в положении [rotation]. */
    fun lock(rotation: Int)
}
