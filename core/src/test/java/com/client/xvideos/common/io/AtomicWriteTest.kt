package com.client.xvideos.common.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class AtomicWriteTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `writeTextAtomically writes content correctly`() {
        val file = File(tmp.newFolder(), "test.txt")
        file.writeTextAtomically("hello atomic")
        assertEquals("hello atomic", file.readText())
    }

    @Test
    fun `writeTextAtomically overwrites existing file`() {
        val file = File(tmp.newFolder(), "test.txt")
        file.writeTextAtomically("first")
        assertEquals("first", file.readText())
        file.writeTextAtomically("second")
        assertEquals("second", file.readText())
    }

    @Test
    fun `writeTextAtomically cleans up temp file on failure`() {
        val folder = tmp.newFolder()
        // Target is an existing directory, so renameTo/delete will fail on directory
        val dirAsTarget = File(folder, "dirTarget").apply {
            mkdirs()
            File(this, "inner.txt").writeText("block deletion")
        }
        val tmpFilesBefore = folder.listFiles { _, name -> name.endsWith(".tmp") }?.size ?: 0

        assertThrows(IOException::class.java) {
            dirAsTarget.writeTextAtomically("content")
        }

        val tmpFilesAfter = folder.listFiles { _, name -> name.endsWith(".tmp") }?.size ?: 0
        assertEquals("Временный .tmp файл должен быть удалён при сбое записи", tmpFilesBefore, tmpFilesAfter)
    }
}
