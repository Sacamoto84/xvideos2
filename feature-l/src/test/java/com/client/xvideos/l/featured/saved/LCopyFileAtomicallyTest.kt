package com.client.xvideos.l.featured.saved

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LCopyFileAtomicallyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun partFiles(dir: File) = dir.listFiles()?.filter { it.isPartialDownload() }.orEmpty()

    @Test
    fun `файл копируется целиком и временного файла не остаётся`() {
        val source = tmp.newFile("source.jpg").apply { writeBytes(ByteArray(4096) { it.toByte() }) }
        val target = File(tmp.newFolder("item"), "media.jpg")

        lCopyFileAtomically(source, target)

        assertArrayEquals(source.readBytes(), target.readBytes())
        assertTrue(partFiles(target.parentFile!!).isEmpty())
    }

    @Test
    fun `прежний файл заменяется новым`() {
        val source = tmp.newFile("source.jpg").apply { writeText("new content") }
        val target = File(tmp.newFolder("item"), "media.jpg").apply { writeText("old") }

        lCopyFileAtomically(source, target)

        assertEquals("new content", target.readText())
    }

    @Test
    fun `сбой копирования не оставляет ни целевого, ни временного файла`() {
        val missing = File(tmp.root, "missing.jpg")
        val folder = tmp.newFolder("item")
        val target = File(folder, "media.jpg")

        val error = runCatching { lCopyFileAtomically(missing, target) }.exceptionOrNull()

        assertTrue("ожидалась ошибка копирования", error != null)
        assertFalse(target.exists())
        assertTrue(partFiles(folder).isEmpty())
    }
}
