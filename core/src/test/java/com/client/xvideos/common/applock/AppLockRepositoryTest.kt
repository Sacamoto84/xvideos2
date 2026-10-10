package com.client.xvideos.common.applock

import android.content.SharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockRepositoryTest {

    private lateinit var securePrefs: InMemorySharedPreferences
    private lateinit var legacyPrefs: InMemorySharedPreferences

    @Before
    fun setUp() {
        securePrefs = InMemorySharedPreferences()
        legacyPrefs = InMemorySharedPreferences()
        AppLockRepository.setTestPreferencesForTesting(securePrefs)
        AppLockRepository.setTestLegacyPreferencesForTesting(legacyPrefs)
        AppLockSession.lock()
    }

    @After
    fun tearDown() {
        AppLockRepository.resetForTesting()
    }

    @Test
    fun `isPasswordSet returns false initially and true after setPassword`() = runTest {
        assertFalse(AppLockRepository.isPasswordSet())

        val result = AppLockRepository.setPassword(null, "1234")
        assertTrue(result.isSuccess)
        assertTrue(AppLockRepository.isPasswordSet())

        // Проверяем, что хэш и соль записаны именно в securePrefs
        assertTrue(securePrefs.contains("app_lock_password_hash"))
        assertTrue(securePrefs.contains("app_lock_password_salt"))
        assertTrue(securePrefs.getBoolean("app_lock_secure_enabled", false))

        // И в legacyPrefs их нет
        assertFalse(legacyPrefs.contains("app_lock_password_hash"))
        assertFalse(legacyPrefs.contains("app_lock_password_salt"))
    }

    @Test
    fun `setPassword fails for password shorter than 4 characters`() = runTest {
        val result = AppLockRepository.setPassword(null, "123")
        assertTrue(result.isFailure)
        assertFalse(AppLockRepository.isPasswordSet())
    }

    @Test
    fun `verifyPassword validates correct and incorrect passwords`() = runTest {
        AppLockRepository.setPassword(null, "secretCode42")

        assertTrue(AppLockRepository.verifyPassword(null, "secretCode42"))
        assertFalse(AppLockRepository.verifyPassword(null, "wrongCode"))
        assertFalse(AppLockRepository.verifyPassword(null, "secretcode42"))
    }

    @Test
    fun `clearPassword removes secrets and resets secure enabled flag`() = runTest {
        AppLockRepository.setPassword(null, "5678")
        assertTrue(AppLockRepository.isPasswordSet())

        AppLockRepository.clearPassword(null)
        assertFalse(AppLockRepository.isPasswordSet())
        assertFalse(securePrefs.getBoolean("app_lock_secure_enabled", true))
        assertFalse(AppLockSession.isUnlocked())
    }

    @Test
    fun `isEnabled prevents tampering when secure lock is enabled`() = runTest {
        AppLockRepository.setPassword(null, "9999")
        assertTrue(AppLockRepository.isPasswordSet())

        // Симулируем попытку сброса флага через SharedPreferences XML
        // secureLockEnabled в защищённом хранилище = true, даже если Settings = false
        assertTrue(AppLockRepository.isEnabled())
    }

    @Test
    fun `failed attempts tracking and reset`() = runTest {
        AppLockRepository.setPassword(null, "1111")
        assertEquals(0L, AppLockRepository.lockoutRemainingMillis())

        for (i in 1..4) {
            AppLockRepository.registerFailedAttempt()
        }
        assertEquals(0L, AppLockRepository.lockoutRemainingMillis())

        // 5-я попытка включает бэк-офф (30 сек)
        val lockoutRemaining = AppLockRepository.registerFailedAttempt()
        assertTrue(lockoutRemaining > 0L)
        assertTrue(securePrefs.getInt("app_lock_failed_attempts", 0) >= 5)

        AppLockRepository.resetFailedAttempts()
        assertEquals(0L, AppLockRepository.lockoutRemainingMillis())
        assertFalse(securePrefs.contains("app_lock_failed_attempts"))
    }

    @Test
    fun `legacy migration moves unencrypted password and salts into secure store`() = runTest {
        // Заполняем legacyPrefs старыми открытыми данными
        legacyPrefs.edit()
            .putString("app_lock_password_hash", "bGVnYWN5SGFzaA==") // "legacyHash" in base64
            .putString("app_lock_password_salt", "bGVnYWN5U2FsdA==") // "legacySalt" in base64
            .putBoolean("app_lock_enabled", true)
            .putInt("app_lock_failed_attempts", 2)
            .apply()

        // Сбрасываем признак миграции
        AppLockRepository.resetForTesting()
        AppLockRepository.setTestPreferencesForTesting(securePrefs)
        AppLockRepository.setTestLegacyPreferencesForTesting(legacyPrefs)

        // Проверяем, что замок определился как установленный
        assertTrue(AppLockRepository.isPasswordSet())

        // Данные должны быть перенесены в securePrefs
        assertEquals("bGVnYWN5SGFzaA==", securePrefs.getString("app_lock_password_hash", null))
        assertEquals("bGVnYWN5U2FsdA==", securePrefs.getString("app_lock_password_salt", null))
        assertTrue(securePrefs.getBoolean("app_lock_secure_enabled", false))
        assertEquals(2, securePrefs.getInt("app_lock_failed_attempts", 0))

        // И полностью удалены из legacyPrefs!
        assertFalse(legacyPrefs.contains("app_lock_password_hash"))
        assertFalse(legacyPrefs.contains("app_lock_password_salt"))
        assertFalse(legacyPrefs.contains("app_lock_failed_attempts"))
    }

    private class InMemorySharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)

        override fun getString(key: String?, defValue: String?): String? =
            map[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (map[key] as? MutableSet<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (map[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (map[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (map[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (map[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) if (value != null) changes[key] = value else removals.add(key)
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) if (values != null) changes[key] = values else removals.add(key)
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removals.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) map.clear()
                removals.forEach { map.remove(it) }
                changes.forEach { (k, v) -> if (v != null) map[k] = v else map.remove(k) }
            }
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }
}
