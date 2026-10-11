package com.client.xvideos.common.applock

import android.os.Build
import android.os.Debug
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Детектор безопасности рантайм-среды для экрана блокировки.
 *
 * Проверяет:
 * 1. Активное подключение JDWP/Java-отладчика ([Debug.isDebuggerConnected]).
 * 2. Наличие ptrace-трассировщика (GDB, LLDB, Frida) через /proc/self/status (TracerPid != 0).
 * 3. Инъекцию библиотек динамического анализа (Frida, Xposed, LSPosed) в адресное пространство процесса через /proc/self/maps.
 * 4. Наличие стандартных бинарных файлов superuser (su) и признаков тестовой прошивки.
 */
object AppLockSecurityDetector {

    private val SUSPICIOUS_LIBRARIES = listOf(
        "frida-agent",
        "frida-gadget",
        "gum-js-loop",
        "gdbus",
        "libxposed",
        "edxposed",
        "lsposed",
        "substrate"
    )

    private val ROOT_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )

    // Тестовые хуки для JVM-тестов (AtomicReference согласно правилам проекта)
    private val testDebuggerAttachedRef = AtomicReference<Boolean?>(null)
    private val testTracerPidRef = AtomicReference<Int?>(null)
    private val testMapsContentRef = AtomicReference<String?>(null)
    private val testRootDetectedRef = AtomicReference<Boolean?>(null)

    internal fun setTestDebuggerAttached(attached: Boolean?) {
        testDebuggerAttachedRef.set(attached)
    }

    internal fun setTestTracerPid(pid: Int?) {
        testTracerPidRef.set(pid)
    }

    internal fun setTestMapsContent(maps: String?) {
        testMapsContentRef.set(maps)
    }

    internal fun setTestRootDetected(detected: Boolean?) {
        testRootDetectedRef.set(detected)
    }

    internal fun resetForTesting() {
        testDebuggerAttachedRef.set(null)
        testTracerPidRef.set(null)
        testMapsContentRef.set(null)
        testRootDetectedRef.set(null)
    }

    /**
     * Проверяет, подключен ли отладчик (Java/JDWP или нативный ptrace).
     */
    fun isDebuggerAttached(): Boolean {
        testDebuggerAttachedRef.get()?.let { return it }

        val javaDebugger = runCatching { Debug.isDebuggerConnected() || Debug.waitingForDebugger() }.getOrDefault(false)
        if (javaDebugger) return true

        return isTracerAttached()
    }

    /**
     * Проверяет поле TracerPid в /proc/self/status.
     * Если процесс трассируется через ptrace, TracerPid > 0.
     */
    fun isTracerAttached(): Boolean {
        testTracerPidRef.get()?.let { return it > 0 }

        return runCatching {
            val statusFile = File("/proc/self/status")
            if (!statusFile.exists() || !statusFile.canRead()) return false

            statusFile.useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("TracerPid:", ignoreCase = true)) {
                        val pidStr = line.substringAfter(":").trim()
                        val pid = pidStr.toIntOrNull() ?: 0
                        return@useLines pid > 0
                    }
                }
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Проверяет наличие внедренных библиотек Frida / Xposed в памяти процесса через /proc/self/maps.
     */
    fun isHookFrameworkDetected(): Boolean {
        testMapsContentRef.get()?.let { maps ->
            return SUSPICIOUS_LIBRARIES.any { maps.contains(it, ignoreCase = true) }
        }

        return runCatching {
            val mapsFile = File("/proc/self/maps")
            if (!mapsFile.exists() || !mapsFile.canRead()) return false

            mapsFile.useLines { lines ->
                for (line in lines) {
                    if (SUSPICIOUS_LIBRARIES.any { line.contains(it, ignoreCase = true) }) {
                        return@useLines true
                    }
                }
                false
            }
        }.getOrDefault(false)
    }

    /**
     * Проверяет признаки Root-доступа.
     */
    fun isRootDetected(): Boolean {
        testRootDetectedRef.get()?.let { return it }

        // Проверка test-keys сборки
        val testKeys = runCatching {
            Build.TAGS?.contains("test-keys", ignoreCase = true) == true
        }.getOrDefault(false)
        if (testKeys) return true

        // Проверка наличия su в файловой системе
        return runCatching {
            ROOT_PATHS.any { path -> File(path).exists() }
        }.getOrDefault(false)
    }

    /**
     * Проверяет, скомпрометирована ли безопасность процесса активными инструментами анализа/взлома.
     */
    fun isProcessCompromised(): Boolean {
        return isDebuggerAttached() || isHookFrameworkDetected()
    }
}
