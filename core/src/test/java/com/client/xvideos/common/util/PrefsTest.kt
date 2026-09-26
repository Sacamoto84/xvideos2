package com.client.xvideos.common.util

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsTest {

    private class FakePreferences(
        private val storage: MutableMap<String, Any?> = mutableMapOf()
    ) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = HashMap(storage)

        override fun getString(key: String?, defValue: String?): String? {
            if (!storage.containsKey(key)) return defValue
            val value = storage[key] ?: return defValue
            return value as? String ?: throw ClassCastException("Not a String")
        }

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
            storage[key] as? Set<String> ?: defValues

        override fun getInt(key: String?, defValue: Int): Int {
            if (!storage.containsKey(key)) return defValue
            val value = storage[key] ?: return defValue
            return value as? Int ?: throw ClassCastException("Not an Int")
        }

        override fun getLong(key: String?, defValue: Long): Long {
            if (!storage.containsKey(key)) return defValue
            val value = storage[key] ?: return defValue
            return value as? Long ?: throw ClassCastException("Not a Long")
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            if (!storage.containsKey(key)) return defValue
            val value = storage[key] ?: return defValue
            return value as? Float ?: throw ClassCastException("Not a Float")
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            if (!storage.containsKey(key)) return defValue
            val value = storage[key] ?: return defValue
            return value as? Boolean ?: throw ClassCastException("Not a Boolean")
        }

        override fun contains(key: String?): Boolean = storage.containsKey(key)

        override fun edit(): SharedPreferences.Editor = throw UnsupportedOperationException()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        fun put(key: String, value: Any?) {
            storage[key] = value
        }
    }

    @Test
    fun `getStringOrDefault returns stored string or default fallback`() {
        val prefs = FakePreferences()
        prefs.put("title", "Hello")
        prefs.put("count", 42) // Wrong type to verify ClassCastException handling

        assertEquals("Hello", prefs.getStringOrDefault("title", "Fallback"))
        assertEquals("Fallback", prefs.getStringOrDefault("unknown", "Fallback"))
        assertEquals("Fallback", prefs.getStringOrDefault("count", "Fallback"))
        assertEquals("Hello", prefs.getStringOrNull("title"))
        assertNull(prefs.getStringOrNull("unknown"))
        assertNull(prefs.getStringOrNull("count"))
    }

    @Test
    fun `getBooleanOrDefault returns stored boolean or default fallback`() {
        val prefs = FakePreferences()
        prefs.put("flag_true", true)
        prefs.put("flag_false", false)
        prefs.put("not_bool", "string")

        assertTrue(prefs.getBooleanOrDefault("flag_true", false))
        assertFalse(prefs.getBooleanOrDefault("flag_false", true))
        assertTrue(prefs.getBooleanOrDefault("unknown", true))
        assertFalse(prefs.getBooleanOrDefault("not_bool", false))
    }

    @Test
    fun `getIntOrDefault returns stored int or default fallback`() {
        val prefs = FakePreferences()
        prefs.put("speed", 100)
        prefs.put("wrong", "str")

        assertEquals(100, prefs.getIntOrDefault("speed", 0))
        assertEquals(50, prefs.getIntOrDefault("unknown", 50))
        assertEquals(10, prefs.getIntOrDefault("wrong", 10))
    }

    @Test
    fun `getLongOrDefault returns stored long or default fallback`() {
        val prefs = FakePreferences()
        prefs.put("timestamp", 123456789L)
        prefs.put("wrong", true)

        assertEquals(123456789L, prefs.getLongOrDefault("timestamp", 0L))
        assertEquals(999L, prefs.getLongOrDefault("unknown", 999L))
        assertEquals(42L, prefs.getLongOrDefault("wrong", 42L))
    }

    @Test
    fun `getFloatOrDefault returns stored float or default fallback`() {
        val prefs = FakePreferences()
        prefs.put("ratio", 1.5f)
        prefs.put("wrong", 123)

        assertEquals(1.5f, prefs.getFloatOrDefault("ratio", 0f))
        assertEquals(2.0f, prefs.getFloatOrDefault("unknown", 2.0f))
        assertEquals(0.5f, prefs.getFloatOrDefault("wrong", 0.5f))
    }
}
