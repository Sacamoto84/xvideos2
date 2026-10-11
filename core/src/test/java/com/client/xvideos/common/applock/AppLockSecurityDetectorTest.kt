package com.client.xvideos.common.applock

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.client.xvideos.common.AppBuildInfo

class AppLockSecurityDetectorTest {

    @Before
    @After
    fun tearDown() {
        AppLockSecurityDetector.resetForTesting()
        AppLockRepository.resetForTesting()
        AppBuildInfo.resetForTesting()
    }

    @Test
    fun `isDebuggerAttached возвращает true когда подключен отладчик`() {
        AppLockSecurityDetector.setTestDebuggerAttached(true)
        assertTrue(AppLockSecurityDetector.isDebuggerAttached())

        AppLockSecurityDetector.setTestDebuggerAttached(false)
        assertFalse(AppLockSecurityDetector.isDebuggerAttached())
    }

    @Test
    fun `isTracerAttached возвращает true когда TracerPid больше нуля`() {
        AppLockSecurityDetector.setTestTracerPid(1337)
        assertTrue(AppLockSecurityDetector.isTracerAttached())

        AppLockSecurityDetector.setTestTracerPid(0)
        assertFalse(AppLockSecurityDetector.isTracerAttached())
    }

    @Test
    fun `isHookFrameworkDetected детектирует библиотеки Frida и Xposed в maps`() {
        val fridaMaps = """
            7f912000-7f920000 r-xp 00000000 fd:00 12345 /data/local/tmp/re.frida.server/frida-agent-64.so
            7f920000-7f921000 r--p 0000e000 fd:00 12345 /data/local/tmp/re.frida.server/frida-agent-64.so
        """.trimIndent()
        AppLockSecurityDetector.setTestMapsContent(fridaMaps)
        assertTrue(AppLockSecurityDetector.isHookFrameworkDetected())

        val lsposedMaps = """
            7fa10000-7fa20000 r-xp 00000000 fd:00 67890 /data/adb/lspd/liblsposed.so
        """.trimIndent()
        AppLockSecurityDetector.setTestMapsContent(lsposedMaps)
        assertTrue(AppLockSecurityDetector.isHookFrameworkDetected())

        val cleanMaps = """
            7fb10000-7fb20000 r-xp 00000000 fd:00 11111 /system/lib64/libc.so
            7fb20000-7fb30000 r-xp 00000000 fd:00 22222 /system/lib64/libart.so
        """.trimIndent()
        AppLockSecurityDetector.setTestMapsContent(cleanMaps)
        assertFalse(AppLockSecurityDetector.isHookFrameworkDetected())
    }

    @Test
    fun `isRootDetected возвращает заданный статус проверки`() {
        AppLockSecurityDetector.setTestRootDetected(true)
        assertTrue(AppLockSecurityDetector.isRootDetected())

        AppLockSecurityDetector.setTestRootDetected(false)
        assertFalse(AppLockSecurityDetector.isRootDetected())
    }

    @Test
    fun `isProcessCompromised объединяет проверку отладчика и хук-фреймворков`() {
        AppLockSecurityDetector.setTestDebuggerAttached(false)
        AppLockSecurityDetector.setTestTracerPid(0)
        AppLockSecurityDetector.setTestMapsContent("clean")
        assertFalse(AppLockSecurityDetector.isProcessCompromised())

        // При активном отладчике
        AppLockSecurityDetector.setTestDebuggerAttached(true)
        assertTrue(AppLockSecurityDetector.isProcessCompromised())

        // При обнаруженной Frida в памяти
        AppLockSecurityDetector.setTestDebuggerAttached(false)
        AppLockSecurityDetector.setTestMapsContent("/data/local/tmp/frida-gadget.so")
        assertTrue(AppLockSecurityDetector.isProcessCompromised())
    }

    @Test
    fun `verifyPassword отклоняет проверку кода при активном Frida или отладчике`() = runTest {
        val prefs = AppLockRepositoryTest.InMemorySharedPreferences().apply {
            edit()
                .putString("app_lock_password_hash", "dGVzdEhhc2g=")
                .putString("app_lock_password_salt", "dGVzdFNhbHQ=")
                .putBoolean("app_lock_secure_enabled", true)
                .apply()
        }
        AppLockRepository.setTestPreferencesForTesting(prefs)

        // Имитируем скомпрометированную среду (Frida)
        AppLockSecurityDetector.setTestMapsContent("libfrida-agent.so")

        val result = AppLockRepository.verifyPassword(password = "secret")
        assertFalse("Проверка пароля должна быть отклонена в скомпрометированной среде", result)

        // Проверяем, что была начислена штрафная попытка
        assertEquals(1, prefs.getInt("app_lock_failed_attempts", 0))
    }

    @Test
    fun `isProcessCompromised в debug сборке разрешает отладчик Android Studio`() {
        AppBuildInfo.init(debug = true, versionName = "test")
        AppLockSecurityDetector.setTestDebuggerAttached(true)
        AppLockSecurityDetector.setTestMapsContent("clean")

        assertFalse("В debug-сборке отладчик не должен блокировать процесс", AppLockSecurityDetector.isProcessCompromised())

        // Но фреймворки хукинга (Frida) всё равно блокируются
        AppLockSecurityDetector.setTestMapsContent("frida-agent.so")
        assertTrue(AppLockSecurityDetector.isProcessCompromised())
    }
}
