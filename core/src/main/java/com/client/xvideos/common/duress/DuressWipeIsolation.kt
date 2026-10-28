package com.client.xvideos.common.duress

import android.content.Context
import android.content.SharedPreferences
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.util.defaultSharedPreferences
import timber.log.Timber
import java.io.File

/**
 * Мгновенная изоляция данных приложения при срабатывании кода уничтожения
 * и последующая фоновая очистка.
 *
 * Принцип работы:
 * 1. Мгновенно создаётся файл-маркер [MARKER_FILE_NAME].
 * 2. Все каталоги данных (`files`, `cache`, `databases`, `no_backup`, `app_webview` и др.)
 *    атомарно перемещаются в папку [PENDING_WIPE_DIR_NAME] через `renameTo`.
 * 3. В `shared_prefs` сохраняются только настройки замка ([PRESERVED_SETTINGS_KEYS])
 *    и защищённое Keystore-хранилище `app_lock_secure_prefs`. Все остальные файлы
 *    (включая `secure_credentials.xml`) удаляются.
 * 4. Код уничтожения переносится в код доступа через [AppLockRepository.promoteDuressCodeToAccessCode].
 * 5. Физическое удаление файлов из [PENDING_WIPE_DIR_NAME] выполняется в фоне
 *    нового процесса либо при следующем старте до инициализации хранилища.
 */
object DuressWipeIsolation {

    const val MARKER_FILE_NAME = ".wipe_in_progress"
    const val PENDING_WIPE_DIR_NAME = "pending_wipe"

    val PRESERVED_SETTINGS_KEYS = setOf(
        "app_lock_enabled",
        "app_lock_timeout_seconds",
        "camouflage_calculator_enabled",
        "blur_recent_tasks",
        "keyboard_incognito_enabled",
        "app_lock_armed",
    )

    private val SKIPPED_DATA_DIR_ENTRIES = setOf(
        "shared_prefs",
        PENDING_WIPE_DIR_NAME,
        MARKER_FILE_NAME,
        DuressTokenStore.TOKEN_FILE_NAME,
    )

    /**
     * Выполняет мгновенную изоляцию данных.
     * Возвращает `true`, если данные успешно изолированы.
     */
    fun isolate(
        dataDir: File,
        defaultPrefs: SharedPreferences?,
        securePrefs: SharedPreferences? = null,
        externalDirs: List<File> = emptyList(),
    ): Boolean {
        return isolateInternal(
            dataDir = dataDir,
            defaultPrefs = defaultPrefs,
            securePrefs = securePrefs,
            externalDirs = externalDirs,
            isRetry = false,
        )
    }

    fun isolate(context: Context): Boolean {
        val dataDir = context.dataDir
        val defaultPrefs = runCatching { context.defaultSharedPreferences() }.getOrNull()
        val externalDirs = listOfNotNull(
            context.getExternalFilesDir(null),
            context.externalCacheDir,
        )
        return isolate(
            dataDir = dataDir,
            defaultPrefs = defaultPrefs,
            externalDirs = externalDirs,
        )
    }

    private fun isolateInternal(
        dataDir: File,
        defaultPrefs: SharedPreferences?,
        securePrefs: SharedPreferences?,
        externalDirs: List<File>,
        isRetry: Boolean,
    ): Boolean {
        val markerFile = File(dataDir, MARKER_FILE_NAME)
        runCatching {
            markerFile.writeText("wipe_in_progress", Charsets.UTF_8)
        }.onFailure {
            Timber.e(it, "DuressWipeIsolation: не удалось создать маркер стирания")
            if (!isRetry) {
                return isolateInternal(dataDir, defaultPrefs, securePrefs, externalDirs, isRetry = true)
            }
            return false
        }

        val pendingWipeDir = File(dataDir, PENDING_WIPE_DIR_NAME).apply { mkdirs() }

        // 1. Очистка shared_prefs: оставляем только замок и разрешённые настройки
        cleanSharedPrefs(dataDir, defaultPrefs)

        // 2. Перемещение каталогов данных в pending_wipe
        var moveFailed = false
        val entries = dataDir.listFiles() ?: emptyArray()
        for (entry in entries) {
            if (entry.name in SKIPPED_DATA_DIR_ENTRIES) continue

            val target = File(pendingWipeDir, entry.name)
            val moved = if (target.exists()) {
                entry.renameTo(File(pendingWipeDir, "${entry.name}_${System.currentTimeMillis()}"))
            } else {
                entry.renameTo(target)
            }

            if (!moved) {
                // Если renameTo не сработал (например, между разными точками монтирования), пробуем рекурсивное удаление
                Timber.w("DuressWipeIsolation: renameTo не сработал для ${entry.name}, пробую deleteRecursively")
                if (!entry.deleteRecursively()) {
                    moveFailed = true
                }
            }
        }

        // 3. Внешние папки приложения
        for (extDir in externalDirs) {
            if (extDir.exists()) {
                extDir.deleteRecursively()
            }
        }

        if (moveFailed) {
            Timber.e("DuressWipeIsolation: изоляция каталогов завершилась с ошибками")
            if (!isRetry) {
                return isolateInternal(dataDir, defaultPrefs, securePrefs, externalDirs, isRetry = true)
            }
            return false
        }

        // 4. Перенос кода уничтожения в код доступа
        val promoted = AppLockRepository.promoteDuressCodeToAccessCode()
        if (!promoted) {
            Timber.w("DuressWipeIsolation: promoteDuressCodeToAccessCode вернул false")
        }

        return true
    }

    private fun cleanSharedPrefs(dataDir: File, defaultPrefs: SharedPreferences?) {
        val sharedPrefsDir = File(dataDir, "shared_prefs")
        if (sharedPrefsDir.exists()) {
            val prefFiles = sharedPrefsDir.listFiles() ?: emptyArray()
            for (file in prefFiles) {
                val name = file.name
                // Сохраняем app_lock_secure_prefs.xml и основной XML настроек
                val isSecurePrefs = name.startsWith("app_lock_secure_prefs")
                val isDefaultPrefs = name.endsWith("_preferences.xml") || name == "mock_prefs.xml"
                if (!isSecurePrefs && !isDefaultPrefs) {
                    file.delete()
                }
            }
        }

        // В основном SharedPreferences очищаем все настройки, кроме белого списка
        if (defaultPrefs != null) {
            val allEntries = defaultPrefs.all
            val preservedValues = mutableMapOf<String, Any?>()
            for (key in PRESERVED_SETTINGS_KEYS) {
                if (allEntries.containsKey(key)) {
                    preservedValues[key] = allEntries[key]
                }
            }

            // Гарантируем, что замок и отметка взвода включены
            preservedValues["app_lock_enabled"] = true
            preservedValues["app_lock_armed"] = true

            defaultPrefs.edit().apply {
                clear()
                for ((k, v) in preservedValues) {
                    when (v) {
                        is Boolean -> putBoolean(k, v)
                        is Int -> putInt(k, v)
                        is Long -> putLong(k, v)
                        is Float -> putFloat(k, v)
                        is String -> putString(k, v)
                    }
                }
            }.commit()
        }
    }

    /**
     * Доочищает остаточные каталоги при старте процесса, если был прерван процесс стирания.
     */
    fun ensureIsolatedBeforeStartup(dataDir: File) {
        val markerFile = File(dataDir, MARKER_FILE_NAME)
        if (!markerFile.exists()) return

        val pendingWipeDir = File(dataDir, PENDING_WIPE_DIR_NAME).apply { mkdirs() }
        val entries = dataDir.listFiles() ?: emptyArray()
        for (entry in entries) {
            if (entry.name in SKIPPED_DATA_DIR_ENTRIES) continue

            val target = File(pendingWipeDir, entry.name)
            val moved = if (target.exists()) {
                entry.renameTo(File(pendingWipeDir, "${entry.name}_${System.currentTimeMillis()}"))
            } else {
                entry.renameTo(target)
            }
            if (!moved) {
                entry.deleteRecursively()
            }
        }
    }

    fun ensureIsolatedBeforeStartup(context: Context) {
        ensureIsolatedBeforeStartup(context.dataDir)
    }

    /**
     * Фоновое физическое удаление изолированных данных из [PENDING_WIPE_DIR_NAME].
     * После успешного удаления снимает файл-маркер [MARKER_FILE_NAME].
     */
    fun cleanPendingWipe(dataDir: File) {
        val pendingWipeDir = File(dataDir, PENDING_WIPE_DIR_NAME)
        if (pendingWipeDir.exists()) {
            pendingWipeDir.deleteRecursively()
        }
        val markerFile = File(dataDir, MARKER_FILE_NAME)
        if (markerFile.exists()) {
            markerFile.delete()
        }
    }

    fun cleanPendingWipe(context: Context) {
        cleanPendingWipe(context.dataDir)
    }

    fun hasInterruptedWipe(context: Context): Boolean {
        return File(context.dataDir, MARKER_FILE_NAME).exists()
    }
}
