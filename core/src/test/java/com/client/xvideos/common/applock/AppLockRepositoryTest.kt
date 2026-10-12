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

        repeat(4) {
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
    fun `перезагрузка и перевод часов вперёд не снимают блокировку ввода`() = runTest {
        AppLockRepository.setPassword(null, "1111")
        val wall = 1_000_000L
        val uptime = 36_000_000L // десять часов с загрузки телефона
        repeat(AppLockThrottle.FREE_ATTEMPTS + 1) {
            AppLockRepository.registerFailedAttempt(wallNow = wall, elapsedNow = uptime)
        }
        assertEquals(
            AppLockThrottle.BASE_LOCKOUT_MS,
            AppLockRepository.lockoutRemainingMillis(wallNow = wall, elapsedNow = uptime)
        )

        // Телефон перезагрузили — монотонные часы пошли заново — и перевели
        // время на сутки вперёд. Раньше оба срока отбрасывались, остаток был 0.
        val afterReboot = 60_000L
        val remaining = AppLockRepository.lockoutRemainingMillis(
            wallNow = wall + 86_400_000L,
            elapsedNow = afterReboot,
        )

        assertEquals(AppLockThrottle.BASE_LOCKOUT_MS, remaining)
        assertEquals(
            afterReboot + AppLockThrottle.BASE_LOCKOUT_MS,
            securePrefs.getLong("app_lock_lockout_until_elapsed", 0L)
        )
        // Срок перенесён один раз, а не при каждой проверке.
        assertEquals(
            AppLockThrottle.BASE_LOCKOUT_MS - 10_000L,
            AppLockRepository.lockoutRemainingMillis(
                wallNow = wall + 86_400_000L,
                elapsedNow = afterReboot + 10_000L,
            )
        )
    }

    @Test
    fun `verifyPasswordThrottled считает ошибки и после предела код не проверяет`() = runTest {
        AppLockRepository.setPassword(null, "2468")

        repeat(AppLockThrottle.FREE_ATTEMPTS) {
            assertEquals(
                AppLockCheckResult.Wrong(lockoutRemainingMs = 0L),
                AppLockRepository.verifyPasswordThrottled(null, "0000")
            )
        }
        val overLimit = AppLockRepository.verifyPasswordThrottled(null, "0000")
        assertTrue(overLimit is AppLockCheckResult.Wrong && overLimit.lockoutRemainingMs > 0L)

        // Блокировка действует: верный код тоже не принимается.
        assertTrue(AppLockRepository.verifyPasswordThrottled(null, "2468") is AppLockCheckResult.LockedOut)
        assertEquals(AppLockThrottle.FREE_ATTEMPTS + 1, securePrefs.getInt("app_lock_failed_attempts", 0))
    }

    @Test
    fun `verifyPasswordThrottled при верном коде сбрасывает счётчик`() = runTest {
        AppLockRepository.setPassword(null, "2468")

        AppLockRepository.verifyPasswordThrottled(null, "0000")
        assertEquals(1, securePrefs.getInt("app_lock_failed_attempts", 0))

        assertEquals(AppLockCheckResult.Success, AppLockRepository.verifyPasswordThrottled(null, "2468"))
        assertFalse(securePrefs.contains("app_lock_failed_attempts"))
    }

    @Test
    fun `замок остаётся закрытым, если защищённое хранилище не открылось`() = runTest {
        AppLockRepository.setPassword(null, "1234")
        AppLockSession.lock()

        // Keystore отказал: защищённого хранилища нет, остались только открытые настройки.
        AppLockRepository.setTestPreferencesForTesting(null)

        assertFalse(AppLockRepository.isPasswordSet())
        assertTrue("замок не должен сниматься от сбоя хранилища", AppLockRepository.isEnabled())
        assertTrue(AppLockRepository.isAppLocked())
        assertTrue(AppLockRepository.isSecretLost())
        assertFalse(AppLockRepository.verifyPassword(null, "1234"))

        // Хранилище снова открылось — код на месте, замок работает как обычно.
        AppLockRepository.setTestPreferencesForTesting(securePrefs)

        assertFalse(AppLockRepository.isSecretLost())
        assertTrue(AppLockRepository.verifyPassword(null, "1234"))
    }

    @Test
    fun `замок остаётся закрытым, если хранилище открылось пустым`() = runTest {
        AppLockRepository.setPassword(null, "1234")
        AppLockSession.lock()

        // Файл хранилища удалён или пересоздан: открывается, но кода в нём нет.
        AppLockRepository.setTestPreferencesForTesting(InMemorySharedPreferences())

        assertTrue(AppLockRepository.isEnabled())
        assertTrue(AppLockRepository.isAppLocked())
    }

    @Test
    fun `код, заданный прежней версией, тоже защищён от пропажи хранилища`() {
        // Прежняя версия писала только в защищённое хранилище, без отметки рядом.
        securePrefs.edit()
            .putString("app_lock_password_hash", "dGVzdEhhc2g=")
            .putString("app_lock_password_salt", "dGVzdFNhbHQ=")
            .putBoolean("app_lock_secure_enabled", true)
            .apply()
        assertTrue(AppLockRepository.isEnabled())

        AppLockRepository.setTestPreferencesForTesting(null)

        assertTrue(AppLockRepository.isEnabled())
    }

    @Test
    fun `после отключения кода пропажа хранилища замок не закрывает`() = runTest {
        AppLockRepository.setPassword(null, "1234")
        AppLockRepository.clearPassword(null)

        AppLockRepository.setTestPreferencesForTesting(null)

        assertFalse(AppLockRepository.isEnabled())
        assertFalse(AppLockRepository.isAppLocked())
    }

    @Test
    fun `код, не записавшийся на диск, замок не взводит`() = runTest {
        AppLockRepository.setTestPreferencesForTesting(InMemorySharedPreferences(failCommits = true))

        val result = AppLockRepository.setPassword(null, "1234")

        assertTrue(result.isFailure)
        assertFalse(AppLockRepository.isPasswordSet())
        assertFalse(AppLockRepository.isEnabled())
        // После перезапуска кода на диске не окажется. Отметка без него закрыла
        // бы приложение навсегда.
        AppLockRepository.setTestPreferencesForTesting(null)
        assertFalse(AppLockRepository.isEnabled())
    }

    @Test
    fun `без заданного кода недоступное хранилище замок не закрывает`() {
        AppLockRepository.setTestPreferencesForTesting(null)

        assertFalse(AppLockRepository.isEnabled())
        assertFalse(AppLockRepository.isAppLocked())
        assertFalse(AppLockRepository.isSecretLost())
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

    @Test
    fun `isAppLocked возвращает false если пароль не задан`() {
        val prefs = InMemorySharedPreferences()
        AppLockRepository.setTestPreferencesForTesting(prefs)
        AppLockSession.lock()

        assertFalse(AppLockRepository.isAppLocked())
    }

    @Test
    fun `isAppLocked возвращает true если замок включен и сессия заблокирована`() {
        val prefs = InMemorySharedPreferences().apply {
            edit()
                .putString("app_lock_password_hash", "dGVzdEhhc2g=")
                .putString("app_lock_password_salt", "dGVzdFNhbHQ=")
                .putBoolean("app_lock_secure_enabled", true)
                .apply()
        }
        AppLockRepository.setTestPreferencesForTesting(prefs)
        AppLockSession.lock()

        assertTrue(AppLockRepository.isAppLocked())
        assertTrue(AppLockRepository.shouldShowLock())
    }

    @Test
    fun `isAppLocked возвращает false если замок включен и сессия разблокирована`() {
        val prefs = InMemorySharedPreferences().apply {
            edit()
                .putString("app_lock_password_hash", "dGVzdEhhc2g=")
                .putString("app_lock_password_salt", "dGVzdFNhbHQ=")
                .putBoolean("app_lock_secure_enabled", true)
                .apply()
        }
        AppLockRepository.setTestPreferencesForTesting(prefs)
        AppLockSession.unlock()

        assertFalse(AppLockRepository.isAppLocked())
        assertFalse(AppLockRepository.shouldShowLock())
    }

    @Test
    fun `isAppLocked блокирует сессию если таймаут в фоне истёк`() {
        val prefs = InMemorySharedPreferences().apply {
            edit()
                .putString("app_lock_password_hash", "dGVzdEhhc2g=")
                .putString("app_lock_password_salt", "dGVzdFNhbHQ=")
                .putBoolean("app_lock_secure_enabled", true)
                .apply()
        }
        AppLockRepository.setTestPreferencesForTesting(prefs)
        AppLockSession.unlock()

        val bgTime = 10_000L
        AppLockSession.onAppBackgrounded(elapsedNow = bgTime)

        var currentTime = bgTime + 5_000L
        AppLockRepository.setTestElapsedRealtimeForTesting { currentTime }

        // До таймаута — не заблокировано
        assertFalse(AppLockRepository.isAppLocked())

        // После таймаута (по умолчанию 60с - MINUTES_1) — блокируется
        currentTime = bgTime + 65_000L
        assertTrue(AppLockRepository.isAppLocked())
        assertFalse(AppLockSession.isUnlocked())
    }

    /**
     * @param failCommits `commit()` сообщает об отказе, как при сбое записи на
     *   диск. В памяти значения при этом меняются — так ведёт себя и настоящая
     *   реализация.
     */
    internal class InMemorySharedPreferences(
        private val failCommits: Boolean = false
    ) : SharedPreferences {
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
                return !failCommits
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
