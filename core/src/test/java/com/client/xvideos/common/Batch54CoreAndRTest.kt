package com.client.xvideos.common

import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.connectivityObserver.hasConnection
import com.client.xvideos.common.kdownloader.Constants
import com.client.xvideos.common.kdownloader.DownloaderConfig
import com.client.xvideos.common.kdownloader.Status
import com.client.xvideos.common.kdownloader.database.DownloadModel
import com.client.xvideos.common.storage.StorageCleanupGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch54CoreAndRTest {

    @Test
    fun `Status state helpers`() {
        assertTrue(Status.FAILED.isFailed)
        assertFalse(Status.COMPLETED.isFailed)

        assertTrue(Status.CANCELLED.isCancelled)
        assertFalse(Status.RUNNING.isCancelled)

        assertTrue(Status.RUNNING.isActive)
        assertTrue(Status.QUEUED.isActive)
        assertFalse(Status.COMPLETED.isActive)
        assertFalse(Status.FAILED.isActive)

        assertTrue(Status.COMPLETED.isTerminal)
        assertTrue(Status.FAILED.isTerminal)
        assertTrue(Status.CANCELLED.isTerminal)
        assertFalse(Status.RUNNING.isTerminal)
        assertFalse(Status.UNKNOWN.isTerminal)
    }

    @Test
    fun `DownloadModel validation and progress calculations`() {
        val invalidModel = DownloadModel(id = 0, url = "")
        assertFalse(invalidModel.isValid)
        assertFalse(invalidModel.hasTotalBytes)
        assertEquals(0L, invalidModel.remainingBytes)
        assertEquals(0f, invalidModel.progressFraction, 0.001f)

        val validModel = DownloadModel(
            id = 42,
            url = "https://cdn.example.com/file.mp4",
            totalBytes = 1000L,
            downloadedBytes = 250L
        )
        assertTrue(validModel.isValid)
        assertTrue(validModel.hasTotalBytes)
        assertEquals(750L, validModel.remainingBytes)
        assertEquals(0.25f, validModel.progressFraction, 0.001f)

        val finishedModel = DownloadModel(
            id = 43,
            url = "https://cdn.example.com/finished.mp4",
            totalBytes = 500L,
            downloadedBytes = 600L
        )
        assertEquals(0L, finishedModel.remainingBytes)
        assertEquals(1.0f, finishedModel.progressFraction, 0.001f)
    }

    @Test
    fun `StorageCleanupGate idle and running status`() = runTest {
        val gate = StorageCleanupGate()
        assertTrue(gate.isIdle)
        assertFalse(gate.isRunning())
        assertFalse(gate.isStarted)
        assertTrue(gate.isCompleted)

        gate.start(this) {
            // Completed cleanup
        }
        gate.await()
        assertTrue(gate.isIdle)
        assertFalse(gate.isRunning())
        assertTrue(gate.isStarted)
        assertTrue(gate.isCompleted)
    }

    @Test
    fun `ConnectivityObserver hasConnection extension`() {
        val connectedState = MutableStateFlow(true)
        val observer = object : ConnectivityObserver {
            override val isConnected: StateFlow<Boolean> = connectedState
        }
        assertTrue(observer.hasConnection())

        connectedState.value = false
        assertFalse(observer.hasConnection())
    }

    @Test
    fun `DownloaderConfig default properties and helper`() {
        val config = DownloaderConfig()
        assertTrue(config.isDefaultTimeouts)
        assertEquals(Constants.DEFAULT_CONNECT_TIMEOUT_IN_MILLS, config.connectTimeOut)
        assertEquals(Constants.DEFAULT_READ_TIMEOUT_IN_MILLS, config.readTimeOut)
        assertEquals(DownloaderConfig.DEFAULT.connectTimeOut, config.connectTimeOut)

        val customConfig = DownloaderConfig(connectTimeOut = 5000, readTimeOut = 10000)
        assertFalse(customConfig.isDefaultTimeouts)
    }
}
