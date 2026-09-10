package com.client.xvideos.r.common.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadedVideoKeysTest {

    @Test
    fun `ключ собирается из креатора и id`() {
        assertEquals("Creator/AbcDef", downloadedVideoKey("Creator", "AbcDef"))
    }

    @Test
    fun `ключи файлов берут креатора из имени папки`() {
        val files = listOf(
            File("/cache/r/Creator/AbcDef.mp4"),
            File("/cache/r/Other/Xyz.mp4"),
        )
        assertEquals(setOf("Creator/AbcDef", "Other/Xyz"), downloadedVideoKeys(files))
    }

    @Test
    fun `файл без родительской папки не роняет разбор`() {
        assertEquals(setOf("/AbcDef"), downloadedVideoKeys(listOf(File("AbcDef.mp4"))))
    }

    @Test
    fun `набор ключей отвечает на проверку скачанности`() {
        val keys = downloadedVideoKeys(listOf(File("/cache/r/Creator/AbcDef.mp4")))
        assertTrue(downloadedVideoKey("Creator", "AbcDef") in keys)
        assertFalse(downloadedVideoKey("Creator", "Missing") in keys)
    }
}
