package com.client.xvideos.common.duress

import android.annotation.SuppressLint
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
            context = null,
            isRetry = false,
        )
    }

    fun isolate(context: Context): Boolean {
        val dataDir = context.dataDir
        val defaultPrefs = runCatching { context.defaultSharedPreferences() }.getOrNull()
        val externalDirs = externalDirsFor(context)
        return isolateInternal(
            dataDir = dataDir,
            defaultPrefs = defaultPrefs,
            securePrefs = null,
            externalDirs = externalDirs,
            context = context,
            isRetry = false,
        )
    }

    private fun isolateInternal(
        dataDir: File,
        defaultPrefs: SharedPreferences?,
        securePrefs: SharedPreferences?,
        externalDirs: List<File>,
        context: Context?,
        isRetry: Boolean,
    ): Boolean {
        val markerFile = File(dataDir, MARKER_FILE_NAME)
        val markerCreated = runCatching {
            markerFile.writeText("wipe_in_progress", Charsets.UTF_8)
        }.onFailure {
            Timber.e(it, "DuressWipeIsolation: не удалось создать маркер стирания")
        }.isSuccess
        if (!markerCreated) {
            return retryOrFail(dataDir, defaultPrefs, securePrefs, externalDirs, context, isRetry)
        }

        val pendingWipeDir = File(dataDir, PENDING_WIPE_DIR_NAME).apply { mkdirs() }
        if (!pendingWipeDir.isDirectory) {
            Timber.e("DuressWipeIsolation: не удалось создать каталог $PENDING_WIPE_DIR_NAME")
            return retryOrFail(dataDir, defaultPrefs, securePrefs, externalDirs, context, isRetry)
        }

        // 1. Перемещение каталогов данных в pending_wipe и очистка внешних папок
        val dirsMoved = moveDataDirEntriesToPendingWipe(dataDir, pendingWipeDir)
        val externalCleared = deleteExternalDirs(externalDirs)
        if (!dirsMoved || !externalCleared) {
            Timber.e("DuressWipeIsolation: изоляция каталогов завершилась с ошибками")
            return retryOrFail(dataDir, defaultPrefs, securePrefs, externalDirs, context, isRetry)
        }

        // 2. Очистка shared_prefs: оставляем только замок и разрешённые настройки
        if (!cleanSharedPrefs(dataDir, defaultPrefs)) {
            Timber.e("DuressWipeIsolation: не удалось синхронно сохранить очищенные настройки")
            return retryOrFail(dataDir, defaultPrefs, securePrefs, externalDirs, context, isRetry)
        }

        // 3. Перенос кода уничтожения в код доступа
        val promoted = AppLockRepository.promoteDuressCode(
            context = context,
            securePrefsOverride = securePrefs,
        )
        if (!promoted) {
            Timber.e("DuressWipeIsolation: promoteDuressCode вернул false")
            return false
        }

        return true
    }

    private fun retryOrFail(
        dataDir: File,
        defaultPrefs: SharedPreferences?,
        securePrefs: SharedPreferences?,
        externalDirs: List<File>,
        context: Context?,
        isRetry: Boolean,
    ): Boolean {
        if (isRetry) return false
        return isolateInternal(
            dataDir = dataDir,
            defaultPrefs = defaultPrefs,
            securePrefs = securePrefs,
            externalDirs = externalDirs,
            context = context,
            isRetry = true,
        )
    }

    private fun moveDataDirEntriesToPendingWipe(dataDir: File, pendingWipeDir: File): Boolean {
        var allOk = true
        val entries = dataDir.listFiles() ?: emptyArray()
        for (entry in entries) {
            if (entry.name in SKIPPED_DATA_DIR_ENTRIES) continue

            val target = uniqueCollisionTarget(pendingWipeDir, entry.name)
            val moved = entry.renameTo(target)
            if (!moved) {
                // Если renameTo не сработал (например, между разными точками монтирования), пробуем рекурсивное удаление
                Timber.w("DuressWipeIsolation: renameTo не сработал для ${entry.name}, пробую deleteRecursively")
                if (!entry.deleteRecursively()) {
                    allOk = false
                }
            }
        }
        return allOk
    }

    private fun uniqueCollisionTarget(pendingWipeDir: File, baseName: String): File {
        val direct = File(pendingWipeDir, baseName)
        if (!direct.exists()) return direct
        var suffix = 1
        while (true) {
            val candidate = File(pendingWipeDir, "${baseName}_$suffix")
            if (!candidate.exists()) return candidate
            suffix++
        }
    }

    private fun deleteExternalDirs(externalDirs: List<File>): Boolean {
        var allCleared = true
        for (extDir in externalDirs) {
            if (extDir.exists() && !extDir.deleteRecursively()) {
                Timber.w("DuressWipeIsolation: не удалось удалить внешнюю папку ${extDir.name}")
                allCleared = false
            }
        }
        return allCleared
    }

    private fun externalDirsFor(context: Context): List<File> = listOfNotNull(
        context.getExternalFilesDir(null),
        context.externalCacheDir,
    )

    private fun cleanSharedPrefsDirectory(dataDir: File) {
        val sharedPrefsDir = File(dataDir, "shared_prefs")
        if (!sharedPrefsDir.exists()) return
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

    // Проверяем Boolean результат commit(), KTX edit(commit = true) возвращает Unit.
    @SuppressLint("UseKtx")
    private fun cleanSharedPrefs(dataDir: File, defaultPrefs: SharedPreferences?): Boolean {
        cleanSharedPrefsDirectory(dataDir)
        if (defaultPrefs == null) return true

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

        val editor = defaultPrefs.edit()
        editor.clear()
        for ((k, v) in preservedValues) {
            when (v) {
                is Boolean -> editor.putBoolean(k, v)
                is Int -> editor.putInt(k, v)
                is Long -> editor.putLong(k, v)
                is Float -> editor.putFloat(k, v)
                is String -> editor.putString(k, v)
            }
        }
        return editor.commit()
    }

    /**
     * Доочищает остаточные каталоги при старте процесса, если был прерван процесс стирания.
     */
    fun ensureIsolatedBeforeStartup(dataDir: File) {
        val markerFile = File(dataDir, MARKER_FILE_NAME)
        if (!markerFile.exists()) return

        val pendingWipeDir = File(dataDir, PENDING_WIPE_DIR_NAME).apply { mkdirs() }
        moveDataDirEntriesToPendingWipe(dataDir, pendingWipeDir)
        cleanSharedPrefsDirectory(dataDir)
    }

    fun ensureIsolatedBeforeStartup(context: Context) {
        if (!hasInterruptedWipe(context)) return
        ensureIsolatedBeforeStartup(context.dataDir)
        deleteExternalDirs(externalDirsFor(context))
        val defaultPrefs = runCatching { context.defaultSharedPreferences() }.getOrNull()
        cleanSharedPrefs(context.dataDir, defaultPrefs)
    }

    /**
     * Фоновое физическое удаление изолированных данных из [PENDING_WIPE_DIR_NAME].
     * После успешного удаления снимает файл-маркер [MARKER_FILE_NAME].
     */
    fun cleanPendingWipe(dataDir: File) {
        val pendingWipeDir = File(dataDir, PENDING_WIPE_DIR_NAME)
        val pendingRemoved = !pendingWipeDir.exists() || pendingWipeDir.deleteRecursively()
        if (!pendingRemoved) {
            Timber.w("DuressWipeIsolation: не удалось полностью удалить $PENDING_WIPE_DIR_NAME")
            return
        }
        val markerFile = File(dataDir, MARKER_FILE_NAME)
        if (markerFile.exists() && !markerFile.delete()) {
            Timber.w("DuressWipeIsolation: не удалось удалить файл-маркер $MARKER_FILE_NAME")
        }
    }

    fun cleanPendingWipe(context: Context) {
        if (hasInterruptedWipe(context)) {
            deleteExternalDirs(externalDirsFor(context))
        }
        cleanPendingWipe(context.dataDir)
    }

    fun hasInterruptedWipe(context: Context): Boolean {
        return File(context.dataDir, MARKER_FILE_NAME).exists()
    }
}
