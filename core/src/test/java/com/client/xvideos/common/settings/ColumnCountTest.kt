package com.client.xvideos.common.settings

import com.client.xvideos.common.applock.AppLockRepositoryTest.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class ColumnCountTest {
    @Test
    fun `an old single column selection starts at two and cycles through all three modes`() {
        val prefs = InMemorySharedPreferences().apply {
            edit().putInt("columns", 1).apply()
        }
        val setting = prefs.columnCountSetting("columns")

        assertEquals(2, setting.field.value)
        for (expected in listOf(3, 4, 2)) {
            advanceColumnCount(setting)
            assertEquals(expected, setting.field.value)
            assertEquals(expected, prefs.getInt("columns", 0))
        }
    }

    @Test
    fun `valid saved selections are preserved and unsupported values use two columns`() {
        val prefs = InMemorySharedPreferences()
        assertEquals(2, prefs.columnCountSetting("missing").field.value)
        for (saved in listOf(-1, 0, 1, 2, 3, 4, 5)) {
            prefs.edit().putInt("columns", saved).apply()
            val setting = prefs.columnCountSetting("columns")
            assertEquals(if (saved in 2..4) saved else 2, setting.field.value)
            setting.clear()
        }
    }
}
