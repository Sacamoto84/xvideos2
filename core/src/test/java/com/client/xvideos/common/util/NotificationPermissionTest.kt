package com.client.xvideos.common.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Когда разрешение на уведомления нужно спросить.
 *
 * Оно было объявлено в манифесте, но не запрашивалось нигде: на Android 13 и
 * новее уведомление веб-сервера с кнопкой «Остановить» не показывалось.
 */
class NotificationPermissionTest {

    @Test
    fun `на Android 13 и новее без разрешения — спрашиваем`() {
        assertTrue(NotificationPermission.shouldRequest(sdkInt = 33, granted = false))
        assertTrue(NotificationPermission.shouldRequest(sdkInt = 36, granted = false))
    }

    @Test
    fun `выданное разрешение повторно не спрашиваем`() {
        assertFalse(NotificationPermission.shouldRequest(sdkInt = 34, granted = true))
    }

    @Test
    fun `до Android 13 разрешения нет — спрашивать нечего`() {
        assertFalse(NotificationPermission.shouldRequest(sdkInt = 32, granted = false))
        assertFalse(NotificationPermission.shouldRequest(sdkInt = 29, granted = false))
    }
}
