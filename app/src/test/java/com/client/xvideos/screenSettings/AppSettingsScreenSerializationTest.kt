package com.client.xvideos.screenSettings

import com.client.xvideos.HapticDemoScreen
import com.client.xvideos.screenRoot.MenuScreen
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class AppSettingsScreenSerializationTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos -> oos.writeObject(value) }
            baos.toByteArray()
        }
        return ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois -> ois.readObject() as T }
        }
    }

    @Test
    fun `AppSettingsScreen serializes and deserializes properly with stable key`() {
        val restored = roundTrip(AppSettingsScreen)
        assertEquals("AppSettingsScreen", restored.key)
        assertEquals(AppSettingsScreen, restored)
    }

    @Test
    fun `HapticDemoScreen serializes and deserializes properly with stable key`() {
        val restored = roundTrip(HapticDemoScreen)
        assertEquals("HapticDemoScreen", restored.key)
        assertEquals(HapticDemoScreen, restored)
    }

    @Test
    fun `MenuScreen serializes and deserializes properly with stable key`() {
        val restored = roundTrip(MenuScreen)
        assertEquals("MenuScreen", restored.key)
        assertEquals(MenuScreen, restored)
    }

    @Test
    fun `Settings back navigation hierarchy priority`() {
        fun resolveBackAction(isScrolled: Boolean, page: SettingsPage): String {
            return when {
                isScrolled -> "SCROLL_TO_TOP"
                page != SettingsPage.Main -> "NAVIGATE_TO_MAIN"
                else -> "POP_SCREEN"
            }
        }

        assertEquals("SCROLL_TO_TOP", resolveBackAction(isScrolled = true, page = SettingsPage.Storage))
        assertEquals("SCROLL_TO_TOP", resolveBackAction(isScrolled = true, page = SettingsPage.Main))
        assertEquals("NAVIGATE_TO_MAIN", resolveBackAction(isScrolled = false, page = SettingsPage.Storage))
        assertEquals("NAVIGATE_TO_MAIN", resolveBackAction(isScrolled = false, page = SettingsPage.Network))
        assertEquals("POP_SCREEN", resolveBackAction(isScrolled = false, page = SettingsPage.Main))
    }
}
