package com.client.xvideos.common

import com.client.xvideos.common.applock.AppLockThrottle
import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.common.backup.XlrBackupReport
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.settings.ThumbnailsSize
import com.client.xvideos.common.settings.hasEnabledColumns
import com.client.xvideos.common.settings.isColumnEnabled
import com.client.xvideos.common.settings.validateColumnCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Batch72CoreSettingsAndAppLockTest {

    @Test
    fun `AppLockTimeout companion name lookups and allNames`() {
        assertEquals(AppLockTimeout.IMMEDIATELY, AppLockTimeout.fromNameOrNull("IMMEDIATELY"))
        assertEquals(AppLockTimeout.MINUTES_5, AppLockTimeout.fromNameOrNull("minutes_5"))
        assertNull(AppLockTimeout.fromNameOrNull(null))
        assertNull(AppLockTimeout.fromNameOrNull("invalid"))

        assertEquals(AppLockTimeout.NEVER, AppLockTimeout.fromNameOrDefault("NEVER"))
        assertEquals(AppLockTimeout.DEFAULT, AppLockTimeout.fromNameOrDefault("unknown"))

        assertEquals(5, AppLockTimeout.allNames.size)
        assertTrue(AppLockTimeout.allNames.contains("MINUTES_1"))
    }

    @Test
    fun `AppLockThrottle state and duration calculations`() {
        val initial = AppLockThrottle.State.INITIAL
        assertTrue(initial.isFreeAttemptAvailable)
        assertFalse(initial.isLocked)

        val reset = initial.reset()
        assertEquals(0, reset.attempts)

        assertEquals(0L, AppLockThrottle.calculateLockoutDurationMs(0))
        assertEquals(0L, AppLockThrottle.calculateLockoutDurationMs(4))
        assertEquals(30_000L, AppLockThrottle.calculateLockoutDurationMs(5))
        assertEquals(60_000L, AppLockThrottle.calculateLockoutDurationMs(6))

        val lockedState = AppLockThrottle.State(attempts = 5, lockoutUntilWall = 100_000L, lockoutUntilElapsed = 100_000L)
        val remainingSecs = AppLockThrottle.remainingSeconds(lockedState, wallNow = 85_000L, elapsedNow = 85_000L)
        assertEquals(15L, remainingSecs)
    }

    @Test
    fun `ColumnSelect helpers`() {
        val flags = listOf(false, true, true, false, false)
        assertTrue(hasEnabledColumns(flags))
        assertTrue(isColumnEnabled(flags, 1))
        assertTrue(isColumnEnabled(flags, 2))
        assertFalse(isColumnEnabled(flags, 3))
        assertFalse(isColumnEnabled(flags, 99))

        val emptyFlags = emptyList<Boolean>()
        assertFalse(hasEnabledColumns(emptyFlags))

        assertEquals(1, validateColumnCount(0, 1, 4))
        assertEquals(4, validateColumnCount(5, 1, 4))
        assertEquals(3, validateColumnCount(3, 1, 4))
    }

    @Test
    fun `ScrollButtonEffect subtitles and names`() {
        assertTrue(ScrollButtonEffect.FLAT.hasSubtitle)
        assertTrue(ScrollButtonEffect.BLUR.hasSubtitle)
        assertTrue(ScrollButtonEffect.GLASS.hasSubtitle)

        assertEquals(ScrollButtonEffect.FLAT, ScrollButtonEffect.fromNameOrNull("FLAT"))
        assertEquals(ScrollButtonEffect.GLASS, ScrollButtonEffect.fromNameOrNull("glass"))
        assertNull(ScrollButtonEffect.fromNameOrNull(null))
        assertNull(ScrollButtonEffect.fromNameOrNull("other"))

        assertTrue(ScrollButtonEffect.isValidName("BLUR"))
        assertFalse(ScrollButtonEffect.isValidName("unknown"))

        assertEquals(3, ScrollButtonEffect.allNames.size)
    }

    @Test
    fun `ThumbnailsSize allValues and fromName`() {
        assertTrue(ThumbnailsSize.allValues.contains("small"))
        assertTrue(ThumbnailsSize.allValues.contains("xMax"))

        assertEquals(ThumbnailsSize.SMALL, ThumbnailsSize.fromNameOrNull("SMALL"))
        assertEquals(ThumbnailsSize.XMAX, ThumbnailsSize.fromNameOrNull("xmax"))
        assertNull(ThumbnailsSize.fromNameOrNull(null))

        assertEquals(ThumbnailsSize.DEFAULT, ThumbnailsSize.fromNameOrDefault("unknown"))
        assertEquals(ThumbnailsSize.XMAX, ThumbnailsSize.fromNameOrDefault("XMAX"))
    }

    @Test
    fun `XlrBackupModels report addition and options mutators`() {
        val report1 = XlrBackupReport(files = 10, bytes = 1024L)
        val report2 = XlrBackupReport(files = 5, bytes = 512L)
        val combined = report1.add(report2)

        assertEquals(15, combined.files)
        assertEquals(1536L, combined.bytes)
        assertTrue(combined.hasFiles)
        assertTrue(combined.hasBytes)

        val rootItem = XlrBackupItem(path = "X", title = "Section X", section = "X", files = 2)
        assertTrue(rootItem.isRootSection)
        assertTrue(rootItem.hasFiles)

        val childItem = XlrBackupItem(path = "X/Fav", title = "Favorites", section = "X", parentPath = "X")
        assertFalse(childItem.isRootSection)
        assertFalse(childItem.hasFiles)

        val options = XlrBackupOptions.DEFAULT
        val fullOptions = options.withLMode(XlrBackupContentMode.FULL).withRMode(XlrBackupContentMode.FULL)
        assertTrue(fullOptions.isFullBackup)
    }

    @Test
    fun `AppPath helper methods`() {
        assertFalse(AppPath.exists(null))
        assertFalse(AppPath.exists(""))
        assertFalse(AppPath.isDirectory(null))
        assertFalse(AppPath.isDirectory(""))

        val tempDir = System.getProperty("java.io.tmpdir")
        if (!tempDir.isNullOrBlank()) {
            assertTrue(AppPath.exists(tempDir))
            assertTrue(AppPath.isDirectory(tempDir))
            val child = AppPath.resolveChildFile(tempDir, "test.txt")
            assertNotNull(child)
            assertEquals(File(tempDir, "test.txt").path, child.path)
        }
    }
}
