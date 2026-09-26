package com.client.xvideos.common

import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.common.applock.AppLockThrottle
import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.common.collectionDB.CollectionName
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.download.work.DownloadStatus
import com.client.xvideos.common.download.work.DownloadWorkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class Batch51CoreModelsTest {

    @Test
    fun `AppLockTimeout properties and safe parsing`() {
        assertEquals(AppLockTimeout.MINUTES_1, AppLockTimeout.DEFAULT)
        assertTrue(AppLockTimeout.IMMEDIATELY.isImmediately)
        assertFalse(AppLockTimeout.IMMEDIATELY.isNever)
        assertTrue(AppLockTimeout.IMMEDIATELY.isAutoLocking)

        assertTrue(AppLockTimeout.NEVER.isNever)
        assertFalse(AppLockTimeout.NEVER.isAutoLocking)

        assertEquals(AppLockTimeout.SECONDS_30, AppLockTimeout.fromSeconds(30))
        assertEquals(AppLockTimeout.MINUTES_1, AppLockTimeout.fromSeconds(null))
        assertEquals(AppLockTimeout.MINUTES_1, AppLockTimeout.fromSeconds(999))
        assertEquals(AppLockTimeout.MINUTES_5, AppLockTimeout.fromSeconds(999, default = AppLockTimeout.MINUTES_5))
        assertNull(AppLockTimeout.fromSecondsOrNull(999))
        assertNull(AppLockTimeout.fromSecondsOrNull(null))
    }

    @Test
    fun `AppLockThrottle inspection properties and isLockedOut helper`() {
        val initial = AppLockThrottle.State.INITIAL
        assertFalse(initial.hasAttempts)
        assertEquals(4, initial.remainingFreeAttempts)
        assertFalse(AppLockThrottle.isLockedOut(initial, 1000L, 1000L))

        var state = initial
        repeat(4) {
            state = AppLockThrottle.onFailedAttempt(state.attempts, 1000L, 1000L)
        }
        assertTrue(state.hasAttempts)
        assertEquals(0, state.remainingFreeAttempts)
        assertFalse(AppLockThrottle.isLockedOut(state, 1000L, 1000L))

        // 5th attempt locks out
        state = AppLockThrottle.onFailedAttempt(state.attempts, 1000L, 1000L)
        assertTrue(state.isLocked)
        assertTrue(AppLockThrottle.isLockedOut(state, 1000L, 1000L))
        assertFalse(AppLockThrottle.isLockedOut(state, 1000L + AppLockThrottle.BASE_LOCKOUT_MS + 1L, 1000L + AppLockThrottle.BASE_LOCKOUT_MS + 1L))
    }

    @Test
    fun `CollectionName normalizeOrDefault`() {
        assertEquals("valid_name", CollectionName.normalizeOrDefault(" valid_name ", "default"))
        assertEquals("default", CollectionName.normalizeOrDefault("..", "default"))
        assertEquals("default", CollectionName.normalizeOrDefault(".hidden", "default"))
        assertEquals("default", CollectionName.normalizeOrDefault("invalid/name", "default"))
    }

    @Test
    fun `DownloadStatus and DownloadWorkState activity and byte calculations`() {
        assertTrue(DownloadStatus.ENQUEUED.isActive)
        assertTrue(DownloadStatus.RUNNING.isActive)
        assertFalse(DownloadStatus.SUCCEEDED.isActive)
        assertTrue(DownloadStatus.SUCCEEDED.isTerminal)
        assertTrue(DownloadStatus.FAILED.isTerminal)
        assertTrue(DownloadStatus.CANCELLED.isTerminal)

        val workState = DownloadWorkState(
            workId = UUID.randomUUID(),
            tag = "video_1",
            status = DownloadStatus.RUNNING,
            bytesDownloaded = 200L,
            totalBytes = 1000L
        )
        assertTrue(workState.isActive)
        assertFalse(workState.isTerminal)
        assertTrue(workState.hasTotalBytes)
        assertEquals(800L, workState.remainingBytes)
    }

    @Test
    fun `XlrBackupModels inspection properties`() {
        val item = XlrBackupItem(
            path = "folder/path",
            title = "Title",
            section = "L",
            parentPath = "parent"
        )
        assertTrue(item.isValid)
        assertTrue(item.hasParent)

        val options = XlrBackupOptions(
            lMode = XlrBackupContentMode.FULL,
            rMode = XlrBackupContentMode.MINI
        )
        assertTrue(options.hasFullContent)
        assertTrue(options.hasMiniContent)
        assertFalse(options.isFullBackup)
        assertFalse(options.isMiniBackup)
    }

    @Test
    fun `CollectionGridItem and CollectionEntity inspection`() {
        val item = CollectionGridItem(name = "Col", previewUrl = "https://cdn/p.jpg", itemsCount = 5)
        assertTrue(item.hasPositiveCount)

        val zeroItem = CollectionGridItem(name = "Zero", previewUrl = null, itemsCount = 0)
        assertFalse(zeroItem.hasPositiveCount)

        val entity = CollectionEntity("MyCol", listOf("elem1", "elem2"))
        assertEquals("elem1", entity.firstOrNull())
        assertEquals("elem2", entity.getOrNull(1))
        assertNull(entity.getOrNull(2))
    }
}
