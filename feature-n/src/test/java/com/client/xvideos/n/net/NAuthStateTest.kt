package com.client.xvideos.n.net

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NAuthStateTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("n_auth_test")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
    }

    @Test
    fun `explicit API key replaces a web session even for 32 character keys`() {
        val key = "a".repeat(32)
        NAuthState.onLoginSuccess("User", "old_session")
        NAuthState.setApiKey(key)
        assertEquals(key, NAuthState.currentApiKey)
        assertEquals("", NAuthState.currentSessionCookie)
        assertTrue(NAuthState.isLoggedIn.value)
        NAuthState.logout()
    }

    @Test
    fun `login success updates state and logout resets`() {
        NAuthState.onLoginSuccess(login = "OtakuUser", sessionCookie = "mock_session_123")
        assertTrue(NAuthState.isLoggedIn.value)
        assertEquals("OtakuUser", NAuthState.username.value)
        assertEquals("mock_session_123", NAuthState.currentSessionCookie)

        NAuthState.logout()
        assertFalse(NAuthState.isLoggedIn.value)
        assertEquals("", NAuthState.username.value)
        assertEquals("", NAuthState.currentSessionCookie)
    }

    @Test
    fun `manual auth with cookie and api key`() {
        NAuthState.onManualAuth("sessionid=manual_cookie_456;", "CustomUser")
        assertTrue(NAuthState.isLoggedIn.value)
        assertEquals("CustomUser", NAuthState.username.value)
        assertEquals("manual_cookie_456", NAuthState.currentSessionCookie)

        NAuthState.logout()

        NAuthState.onManualAuth("test_api_key_789", "ApiKeyUser")
        assertTrue(NAuthState.isLoggedIn.value)
        assertEquals("ApiKeyUser", NAuthState.username.value)
        assertEquals("test_api_key_789", NAuthState.currentApiKey)

        NAuthState.logout()
        assertFalse(NAuthState.isLoggedIn.value)
    }
}
