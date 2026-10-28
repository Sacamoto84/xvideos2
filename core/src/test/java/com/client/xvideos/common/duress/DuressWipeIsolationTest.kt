package com.client.xvideos.common.duress

import android.content.SharedPreferences
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockSession
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DuressWipeIsolationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataDir: File
    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var databasesDir: File
    private lateinit var sharedPrefsDir: File

    private lateinit var securePrefs: MockInMemoryPrefs
    private lateinit var defaultPrefs: MockInMemoryPrefs

    @Before
    fun setUp() {
        dataDir = tempFolder.newFolder("mock_data_dir")
        filesDir = File(dataDir, "files").apply { mkdirs() }
        cacheDir = File(dataDir, "cache").apply { mkdirs() }
        databasesDir = File(dataDir, "databases").apply { mkdirs() }
        sharedPrefsDir = File(dataDir, "shared_prefs").apply { mkdirs() }

        securePrefs = MockInMemoryPrefs()
        defaultPrefs = MockInMemoryPrefs()

        AppLockRepository.setTestPreferencesForTesting(securePrefs)
        AppLockRepository.setTestLegacyPreferencesForTesting(defaultPrefs)
        AppLockSession.lock()
    }

    @After
    fun tearDown() {
        AppLockRepository.resetForTesting()
    }

    @Test
    fun `isolateSensitiveData moves sensitive directories into pending_wipe and creates marker`() = runTest {
        // Задаём код доступа и код уничтожения
        AppLockRepository.setPassword(null, "1234")
        AppLockRepository.setDuressCode(null, "9876")

        // Наполняем тестовые папки файлами
        File(filesDir, "secret_data.txt").writeText("secret")
        File(cacheDir, "thumb.jpg").writeText("image")
        File(databasesDir, "downloads.db").writeText("database")

        // Имитируем сохранённые настройки
        defaultPrefs.edit()
            .putBoolean("app_lock_enabled", true)
            .putInt("app_lock_timeout_seconds", 300)
            .putBoolean("camouflage_calculator_enabled", true)
            .putBoolean("blur_recent_tasks", true)
            .putBoolean("keyboard_incognito_enabled", true)
            .putString("unpreserved_user_token", "token123")
            .putString("another_unpreserved_setting", "value")
            .apply()

        // Создаём файл secure_credentials в shared_prefs
        File(sharedPrefsDir, "secure_credentials.xml").writeText("<map/>")
        File(sharedPrefsDir, "custom_plugin_pref.xml").writeText("<map/>")

        val success = DuressWipeIsolation.isolate(
            dataDir = dataDir,
            defaultPrefs = defaultPrefs,
            securePrefs = securePrefs
        )
        assertTrue(success)

        // Маркер и папка pending_wipe созданы
        assertTrue(File(dataDir, DuressWipeIsolation.MARKER_FILE_NAME).exists())
        val pendingWipeDir = File(dataDir, DuressWipeIsolation.PENDING_WIPE_DIR_NAME)
        assertTrue(pendingWipeDir.exists())

        // Исходные файлы/папки перемещены (их больше нет в dataDir напрямую)
        assertFalse(File(filesDir, "secret_data.txt").exists())
        assertFalse(File(cacheDir, "thumb.jpg").exists())
        assertFalse(File(databasesDir, "downloads.db").exists())

        // Но они находятся внутри pending_wipe
        assertTrue(File(pendingWipeDir, "files/secret_data.txt").exists() ||
            pendingWipeDir.walk().any { it.name == "secret_data.txt" })

        // Несохраняемые SharedPreferences удалены
        assertFalse(File(sharedPrefsDir, "secure_credentials.xml").exists())
        assertFalse(File(sharedPrefsDir, "custom_plugin_pref.xml").exists())
        assertFalse(defaultPrefs.contains("unpreserved_user_token"))
        assertFalse(defaultPrefs.contains("another_unpreserved_setting"))

        // Сохраняемые настройки остались на месте
        assertTrue(defaultPrefs.getBoolean("app_lock_enabled", false))
        assertEquals(300, defaultPrefs.getInt("app_lock_timeout_seconds", 0))
        assertTrue(defaultPrefs.getBoolean("camouflage_calculator_enabled", false))
        assertTrue(defaultPrefs.getBoolean("blur_recent_tasks", false))
        assertTrue(defaultPrefs.getBoolean("keyboard_incognito_enabled", false))

        // Код уничтожения перенесён в код доступа!
        assertTrue(AppLockRepository.isPasswordSet())
        assertFalse(AppLockRepository.isDuressCodeSet())
        assertTrue(AppLockRepository.verifyPassword(null, "9876"))
        assertFalse(AppLockRepository.verifyPassword(null, "1234"))
    }

    @Test
    fun `cleanPendingWipe removes pending_wipe and marker file`() {
        File(dataDir, DuressWipeIsolation.MARKER_FILE_NAME).writeText("marker")
        val pendingWipeDir = File(dataDir, DuressWipeIsolation.PENDING_WIPE_DIR_NAME).apply { mkdirs() }
        File(pendingWipeDir, "old_file.bin").writeText("data")

        DuressWipeIsolation.cleanPendingWipe(dataDir)

        assertFalse(pendingWipeDir.exists())
        assertFalse(File(dataDir, DuressWipeIsolation.MARKER_FILE_NAME).exists())
    }

    @Test
    fun `ensureIsolatedBeforeStartup isolates directories if marker exists`() {
        // Маркер существует
        File(dataDir, DuressWipeIsolation.MARKER_FILE_NAME).writeText("marker")
        File(filesDir, "residual.txt").writeText("leftover")

        DuressWipeIsolation.ensureIsolatedBeforeStartup(dataDir)

        // Файлы должны быть перемещены в pending_wipe
        assertFalse(File(filesDir, "residual.txt").exists())
        val pendingWipeDir = File(dataDir, DuressWipeIsolation.PENDING_WIPE_DIR_NAME)
        assertTrue(pendingWipeDir.walk().any { it.name == "residual.txt" })
    }

    @Test
    fun `ensureIsolatedBeforeStartup does nothing if marker does not exist`() {
        File(filesDir, "normal_file.txt").writeText("data")

        DuressWipeIsolation.ensureIsolatedBeforeStartup(dataDir)

        assertTrue(File(filesDir, "normal_file.txt").exists())
        assertFalse(File(dataDir, DuressWipeIsolation.PENDING_WIPE_DIR_NAME).exists())
    }

    internal class MockInMemoryPrefs : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (map[key] as? MutableSet<String> ?: defValues)
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Number)?.toInt() ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Number)?.toLong() ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Number)?.toFloat() ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = MockEditor()

        inner class MockEditor : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) {
                    changes[key] = value
                }
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) {
                    changes[key] = values
                }
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) {
                    changes[key] = value
                }
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) {
                    changes[key] = value
                }
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) {
                    changes[key] = value
                }
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) {
                    changes[key] = value
                }
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) {
                    removals.add(key)
                }
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }
            override fun commit(): Boolean {
                apply(); return true
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
