package com.client.xvideos.common.settings

import android.content.SharedPreferences
import com.client.xvideos.common.settings.element.SettingElementSecureCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingElementSecureCredentialsTest {

    /** Хранилище, которое считает записи: одна запись — один `apply()`. */
    private class CountingPreferences(
        private val storage: MutableMap<String, String?> = mutableMapOf()
    ) : SharedPreferences {
        var writes = 0
            private set

        override fun getString(key: String?, defValue: String?): String? =
            if (storage.containsKey(key)) storage[key] else defValue

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, String?>()

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun apply() {
                storage.putAll(pending)
                writes++
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun putStringSet(key: String?, values: Set<String>?) = unsupported()
            override fun putInt(key: String?, value: Int) = unsupported()
            override fun putLong(key: String?, value: Long) = unsupported()
            override fun putFloat(key: String?, value: Float) = unsupported()
            override fun putBoolean(key: String?, value: Boolean) = unsupported()
            override fun remove(key: String?) = unsupported()
            override fun clear() = unsupported()
        }

        override fun getAll(): MutableMap<String, *> = HashMap(storage)
        override fun contains(key: String?): Boolean = storage.containsKey(key)
        override fun getStringSet(key: String?, defValues: Set<String>?) = unsupported()
        override fun getInt(key: String?, defValue: Int) = unsupported()
        override fun getLong(key: String?, defValue: Long) = unsupported()
        override fun getFloat(key: String?, defValue: Float) = unsupported()
        override fun getBoolean(key: String?, defValue: Boolean) = unsupported()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = unsupported()

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = unsupported()

        private fun unsupported(): Nothing = throw UnsupportedOperationException()
    }

    // Хранилище null: значение живёт в памяти, как в Compose Preview.
    private fun inMemory() = SettingElementSecureCredentials(securePrefs = null, loginKey = "login", passwordKey = "pass")

    @Test
    fun saveReplacesLoginAndPasswordInOneSnapshot() {
        val setting = inMemory()

        setting.save("user", "secret")

        val saved = setting.field.value
        assertEquals("user", saved.login)
        assertEquals("secret", saved.password)
    }

    @Test
    fun saveBumpsRevisionOnce() {
        val setting = inMemory()
        val before = setting.field.value.revision

        setting.save("user", "secret")

        assertEquals(before + 1, setting.field.value.revision)
    }

    @Test
    fun savingSameValuesAgainStillBumpsRevision() {
        val setting = inMemory()
        setting.save("user", "secret")
        val before = setting.field.value.revision

        setting.save("user", "secret")

        assertEquals(before + 1, setting.field.value.revision)
    }

    @Test
    fun clearErasesLoginAndPassword() {
        val setting = inMemory()
        setting.save("user", "secret")

        setting.clear()

        assertEquals("", setting.field.value.login)
        assertEquals("", setting.field.value.password)
    }

    @Test
    fun saveWritesBothKeysInOneStorageWrite() {
        val prefs = CountingPreferences()
        val setting = SettingElementSecureCredentials(prefs, loginKey = "login", passwordKey = "pass")

        setting.save("user", "secret")

        assertEquals(1, prefs.writes)
        assertEquals("user", prefs.getString("login", null))
        assertEquals("secret", prefs.getString("pass", null))
    }

    @Test
    fun storedValuesAreReadOnCreation() {
        val prefs = CountingPreferences(mutableMapOf("login" to "user", "pass" to "secret"))

        val saved = SettingElementSecureCredentials(prefs, loginKey = "login", passwordKey = "pass").field.value

        assertEquals("user", saved.login)
        assertEquals("secret", saved.password)
    }

    @Test
    fun accountIsCompleteOnlyWithBothLoginAndPassword() {
        val setting = inMemory()

        setting.save("user", " ")
        assertFalse(setting.field.value.isComplete)

        setting.save("user", "secret")
        assertTrue(setting.field.value.isComplete)
    }

    @Test
    fun passwordIsNotPrintedByToString() {
        val setting = inMemory()

        setting.save("user", "secret")

        assertFalse(setting.field.value.toString().contains("secret"))
    }
}
