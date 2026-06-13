package com.client.xvideos.common.zip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipUtilsTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `zip then unzip reproduces tree with folder name as top entry`() {
        val src = tmp.newFolder("MyCol")
        File(src, "item1").mkdirs()
        File(src, "item1/media.jpg").writeText("MEDIA")
        File(src, "item1/metadata.json").writeText("{}")
        File(src, "collection.json").writeText("{\"v\":1}")

        val zipFile = File(tmp.newFolder("out"), "MyCol.zip")
        ZipUtils.zipDirectory(src, zipFile)
        assertTrue(zipFile.exists())

        val dest = tmp.newFolder("dest")
        ZipUtils.unzip(zipFile, dest)

        assertEquals("MEDIA", File(dest, "MyCol/item1/media.jpg").readText())
        assertEquals("{}", File(dest, "MyCol/item1/metadata.json").readText())
        assertEquals("{\"v\":1}", File(dest, "MyCol/collection.json").readText())
    }

    @Test
    fun `unzip rejects zip-slip entries`() {
        val evilZip = File(tmp.newFolder("evil"), "evil.zip")
        ZipOutputStream(evilZip.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("../escape.txt"))
            zip.write("x".toByteArray())
            zip.closeEntry()
        }
        val dest = tmp.newFolder("dest2")
        try {
            ZipUtils.unzip(evilZip, dest)
            fail("Expected zip-slip to be rejected")
        } catch (e: IllegalArgumentException) {
            // ожидаемо
        }
    }
}
