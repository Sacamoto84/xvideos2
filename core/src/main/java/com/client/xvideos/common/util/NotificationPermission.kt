package com.client.xvideos.common.util

import android.os.Build

/** Разрешение на показ уведомлений (`POST_NOTIFICATIONS`). */
object NotificationPermission {

    /**
     * Нужно ли спросить разрешение перед тем, как показывать уведомление.
     *
     * До Android 13 разрешения нет — уведомления показываются и так. С Android 13
     * у свежей установки они выключены, пока пользователь не разрешит: объявить
     * разрешение в манифесте мало, его нужно запросить.
     *
     * @param sdkInt Версия Android устройства.
     * @param granted Разрешение уже выдано.
     */
    fun shouldRequest(sdkInt: Int = Build.VERSION.SDK_INT, granted: Boolean): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU && !granted
}
