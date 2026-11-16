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
}
