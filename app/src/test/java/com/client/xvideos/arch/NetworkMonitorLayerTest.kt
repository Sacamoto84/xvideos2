package com.client.xvideos.arch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Монитор сети рисуется поверх подложки статус-бара.
 *
 * `StatusBarHost` кладёт под бар тёмный градиент (0.9 у верхнего края), и всё,
 * что нарисовано раньше него, оказывается под ним. Монитор стоит в той же полосе:
 * пока его вызывали из `ScreenRoot`, подложка закрывала цифры почти целиком.
 */
class NetworkMonitorLayerTest {

    @Test
    fun `монитор сети вызывается после подложки статус-бара`() {
        val code = source("MainActivity.kt")
        val scrim = code.indexOf("StatusBarHost(")
        val monitor = code.indexOf("AppNetworkSpeedMonitorLite()")

        assertTrue("В MainActivity не найден StatusBarHost", scrim >= 0)
        assertTrue("В MainActivity не найден монитор сети", monitor >= 0)
        assertTrue("Монитор сети рисуется раньше подложки статус-бара и уходит под неё", monitor > scrim)
    }

    @Test
    fun `корневой экран монитор не рисует`() {
        assertFalse(
            "ScreenRoot снова рисует монитор сети — он окажется под подложкой статус-бара",
            "AppNetworkSpeedMonitorLite()" in source("screenRoot/ScreenRoot.kt"),
        )
    }

    private fun source(path: String): String =
        ProjectSources.kotlinSourcesWithoutComments().first { it.first == path }.second
}
