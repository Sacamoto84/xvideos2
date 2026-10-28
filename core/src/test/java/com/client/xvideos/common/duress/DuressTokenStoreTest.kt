package com.client.xvideos.common.duress

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DuressTokenStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataDir: File

    @Before
    fun setUp() {
        dataDir = tempFolder.newFolder("mock_data_dir")
        DuressTokenStore.resetForTesting()
    }

    @After
    fun tearDown() {
        DuressTokenStore.resetForTesting()
    }

    @Test
    fun `valid token within lifespan returns true and deletes file`() {
        var elapsed = 10_000L
        var wall = 100_000L
        DuressTokenStore.setTimeProvidersForTesting(
            elapsedProvider = { elapsed },
            wallProvider = { wall }
        )

        val token = DuressTokenStore.createOneTimeToken(dataDir)
        assertTrue(token.isNotBlank())
        assertTrue(File(dataDir, DuressTokenStore.TOKEN_FILE_NAME).exists())

        // 2 секунды спустя
        elapsed += 2_000L
        wall += 2_000L

        val valid = DuressTokenStore.consumeTokenIfValid(dataDir, token)
        assertTrue(valid)
        assertFalse(File(dataDir, DuressTokenStore.TOKEN_FILE_NAME).exists())
    }

    @Test
    fun `second attempt with same token fails because file is already deleted`() {
        val token = DuressTokenStore.createOneTimeToken(dataDir)
        assertTrue(DuressTokenStore.consumeTokenIfValid(dataDir, token))
        assertFalse(DuressTokenStore.consumeTokenIfValid(dataDir, token))
    }

    @Test
    fun `wrong token returns false and deletes file`() {
        val token = DuressTokenStore.createOneTimeToken(dataDir)
        assertFalse(DuressTokenStore.consumeTokenIfValid(dataDir, "invalid-token-123"))
        assertFalse(File(dataDir, DuressTokenStore.TOKEN_FILE_NAME).exists())
    }

    @Test
    fun `null or empty token returns false`() {
        DuressTokenStore.createOneTimeToken(dataDir)
        assertFalse(DuressTokenStore.consumeTokenIfValid(dataDir, null))
        assertFalse(File(dataDir, DuressTokenStore.TOKEN_FILE_NAME).exists())

        DuressTokenStore.createOneTimeToken(dataDir)
        assertFalse(DuressTokenStore.consumeTokenIfValid(dataDir, ""))
        assertFalse(File(dataDir, DuressTokenStore.TOKEN_FILE_NAME).exists())
    }

    @Test
    fun `expired token after 15 seconds returns false`() {
        var elapsed = 10_000L
        var wall = 100_000L
        DuressTokenStore.setTimeProvidersForTesting(
            elapsedProvider = { elapsed },
            wallProvider = { wall }
        )

        val token = DuressTokenStore.createOneTimeToken(dataDir)

        // 16 секунд спустя (> 15_000 мс)
        elapsed += 16_000L
        wall += 16_000L

        assertFalse(DuressTokenStore.consumeTokenIfValid(dataDir, token))
        assertFalse(File(dataDir, DuressTokenStore.TOKEN_FILE_NAME).exists())
    }

    @Test
    fun `device reboot between token creation and check resets elapsedRealtime and invalidates token`() {
        var elapsed = 50_000L
        var wall = 100_000L
        DuressTokenStore.setTimeProvidersForTesting(
            elapsedProvider = { elapsed },
            wallProvider = { wall }
        )

        val token = DuressTokenStore.createOneTimeToken(dataDir)

        // Телефон перезагрузился: elapsedRealtime сбросился в 1000мс
        elapsed = 1_000L
        wall += 1_000L

        assertFalse(DuressTokenStore.consumeTokenIfValid(dataDir, token))
        assertFalse(File(dataDir, DuressTokenStore.TOKEN_FILE_NAME).exists())
    }
}
