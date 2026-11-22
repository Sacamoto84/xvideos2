package com.client.xvideos.common.settings.element

import com.client.xvideos.common.applock.AppLockRepositoryTest.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NAccessSettingsTest {
    @Test
    fun `миграция удаляет открытые значения после подтверждённой записи`() {
        val legacy = InMemorySharedPreferences().apply { map["n_api_key"] = "test-key" }
        val secure = InMemorySharedPreferences()

        assertEquals(NAccessCredentials(apiKey = "test-key"), NAccessSettings(legacy, secure).field.value)
        assertFalse(legacy.contains("n_api_key"))
        assertEquals("test-key", secure.getString("n_api_key", null))

        val settings = NAccessSettings(legacy, secure)
        assertTrue(settings.save(NAccessCredentials(sessionCookie = "test-session")))
        assertEquals(NAccessCredentials(sessionCookie = "test-session"), NAccessSettings(legacy, secure).field.value)
        assertTrue(legacy.all.isEmpty())
    }

    @Test
    fun `неудачная запись сохраняет исходную сессию для следующей миграции`() {
        val legacy = InMemorySharedPreferences().apply { map["n_session_cookie"] = "test-session" }
        val failingSecure = InMemorySharedPreferences(failCommits = true)

        assertEquals(NAccessCredentials(sessionCookie = "test-session"), NAccessSettings(legacy, failingSecure).field.value)
        assertTrue(legacy.contains("n_session_cookie"))
    }

    @Test
    fun `при недоступном хранилище новый ключ остаётся в памяти`() {
        val legacy = InMemorySharedPreferences().apply { map["n_session_cookie"] = "old-session" }
        val settings = NAccessSettings(legacy, null)

        assertFalse(settings.save(NAccessCredentials(apiKey = "test-key")))

        assertEquals("test-key", settings.field.value.apiKey)
        assertFalse(legacy.contains("n_api_key"))
        assertFalse(legacy.contains("n_session_cookie"))
        assertEquals(NAccessCredentials(), NAccessSettings(legacy, null).field.value)
        val secure = InMemorySharedPreferences().apply { map["n_api_key"] = "previous-key" }
        assertEquals(NAccessCredentials(), NAccessSettings(legacy, secure).field.value)
        assertEquals(NAccessCredentials(), NAccessSettings(legacy, secure).field.value)
    }
}
