package com.client.xvideos.common.settings

import com.client.xvideos.common.settings.element.SettingElementSecureString
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingElementSecureStringTest {

    // Хранилище null: значение живёт в памяти, как в Compose Preview.
    private fun setting() = SettingElementSecureString(securePrefs = null, name = "secret")

    @Test
    fun revisionGrowsWhenValueChanges() {
        val setting = setting()
        val before = setting.revision

        setting.setValue("first")

        assertEquals(before + 1, setting.revision)
    }

    @Test
    fun revisionGrowsWhenSameValueIsWrittenAgain() {
        val setting = setting()
        setting.setValue("same")
        val before = setting.revision

        setting.setValue("same")

        assertEquals("same", setting.field.value)
        assertEquals(before + 1, setting.revision)
    }
}
