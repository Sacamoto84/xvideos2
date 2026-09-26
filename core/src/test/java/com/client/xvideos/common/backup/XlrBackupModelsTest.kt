package com.client.xvideos.common.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XlrBackupModelsTest {

    @Test
    fun `XlrBackupReport helpers operate correctly`() {
        val emptyReport = XlrBackupReport.EMPTY
        assertTrue(emptyReport.isEmpty)
        assertFalse(emptyReport.isNotEmpty)

        val report = XlrBackupReport(files = 5, bytes = 1024L)
        assertFalse(report.isEmpty)
        assertTrue(report.isNotEmpty)
    }

    @Test
    fun `XlrBackupItem helpers operate correctly`() {
        val item = XlrBackupItem(path = "/backup/1", title = "Backup 1", section = "l")
        assertTrue(item.isValid)
        assertTrue(item.isEmpty)
        assertFalse(item.isNotEmpty)

        val invalidItem = XlrBackupItem(path = "", title = "", section = "l")
        assertFalse(invalidItem.isValid)

        val populatedItem = item.copy(files = 1, bytes = 500L)
        assertFalse(populatedItem.isEmpty)
        assertTrue(populatedItem.isNotEmpty)
    }

    @Test
    fun `XlrBackupContentMode and XlrBackupOptions operate correctly`() {
        assertEquals(XlrBackupContentMode.MINI, XlrBackupContentMode.DEFAULT)
        assertTrue(XlrBackupContentMode.FULL.isFull)
        assertFalse(XlrBackupContentMode.FULL.isMini)
        assertTrue(XlrBackupContentMode.MINI.isMini)
        assertFalse(XlrBackupContentMode.MINI.isFull)

        val defaultOptions = XlrBackupOptions.DEFAULT
        assertTrue(defaultOptions.isMiniBackup)
        assertFalse(defaultOptions.isFullBackup)

        val fullOptions = XlrBackupOptions.FULL
        assertTrue(fullOptions.isFullBackup)
        assertFalse(fullOptions.isMiniBackup)
    }
}
