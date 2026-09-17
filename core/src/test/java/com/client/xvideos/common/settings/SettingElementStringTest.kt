package com.client.xvideos.common.settings

import android.content.SharedPreferences
import com.client.xvideos.common.settings.element.SettingElementString
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingElementStringTest {

    private class FakePreferences(
        private val storage: MutableMap<String, Any?> = mutableMapOf()
    ) : SharedPreferences {
        private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = HashMap(storage)

        override fun getString(key: String?, defValue: String?): String? {
            return if (storage.containsKey(key)) storage[key] as? String else defValue
        }

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
            storage[key] as? Set<String> ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (storage[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (storage[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (storage[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (storage[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = storage.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(this, storage)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listeners.remove(listener)
        }

        fun notifyChanged(key: String) {
            listeners.toList().forEach { it.onSharedPreferenceChanged(this, key) }
        }

        fun putRaw(key: String, value: Any?) {
            storage[key] = value
        }
    }

    private class FakeEditor(
        private val prefs: FakePreferences,
        private val storage: MutableMap<String, Any?>
    ) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val toRemove = mutableSetOf<String>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) toRemove.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) storage.clear()
            toRemove.forEach { storage.remove(it) }
            temp.forEach { (k, v) ->
                storage[k] = v
                prefs.notifyChanged(k)
            }
        }
    }

    @Test
    fun defaultValueIsUsedWhenKeyAbsent() {
        val prefs = FakePreferences()
        val setting = SettingElementString(prefs, "test_key", default = "my_default")
        assertEquals("my_default", setting.field.value)
    }

    @Test
    fun storedValueIsReturnedWhenKeyExists() {
        val prefs = FakePreferences()
        prefs.putRaw("theme", "dark")
        val setting = SettingElementString(prefs, "theme", default = "light")
        assertEquals("dark", setting.field.value)
    }

    @Test
    fun nullInSharedPrefsFallsBackToDefaultSafely() {
        val prefs = FakePreferences()
        prefs.putRaw("theme", null)
        val setting = SettingElementString(prefs, "theme", default = "light")
        assertEquals("light", setting.field.value)
    }

    @Test
    fun setValueUpdatesFlowAndPreferences() {
        val prefs = FakePreferences()
        val setting = SettingElementString(prefs, "lang", default = "en")
        setting.setValue("ru")
        assertEquals("ru", setting.field.value)
        assertEquals("ru", prefs.getString("lang", "en"))
    }

    @Test
    fun externalChangeListenerUpdatesFlow() {
        val prefs = FakePreferences()
        val setting = SettingElementString(prefs, "mode", default = "auto")
        assertEquals("auto", setting.field.value)

        prefs.edit().putString("mode", "manual").apply()
        assertEquals("manual", setting.field.value)
    }

    @Test
    fun clearUnregistersListener() {
        val prefs = FakePreferences()
        val setting = SettingElementString(prefs, "mode", default = "auto")
        setting.clear()
        prefs.edit().putString("mode", "manual").apply()
        assertEquals("auto", setting.field.value)
    }
}
