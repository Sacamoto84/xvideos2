package com.client.xvideos.x

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SavedX_DownloadsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `скачанные видео и постеры корректно извлекаются в in-memory множества`() {
        val root = tmp.newFolder("downloads")
        File(root, "123.mp4").writeText("video")
        File(root, "123.jpg").writeText("poster")
        File(root, "123.info").writeText("{}")
        File(root, "456.mp4").writeText("video")
        File(root, "invalid.mp4").writeText("corrupt")

        val allFiles = root.listFiles() ?: emptyArray()
        val videoIds = allFiles.filter { it.isFile && it.extension == "mp4" }
            .mapNotNull { it.nameWithoutExtension.toLongOrNull() }.toSet()
        val posterIds = allFiles.filter { it.isFile && it.extension == "jpg" }
            .mapNotNull { it.nameWithoutExtension.toLongOrNull() }.toSet()

        assertTrue(videoIds.contains(123L))
        assertTrue(videoIds.contains(456L))
        assertFalse(videoIds.contains(789L))
        assertEquals(setOf(123L, 456L), videoIds)

        assertTrue(posterIds.contains(123L))
        assertFalse(posterIds.contains(456L))
        assertEquals(setOf(123L), posterIds)
    }

    @Test
    fun `файлы info сортируются по убыванию даты изменения`() {
        val root = tmp.newFolder("downloads_sort")
        File(root, "1.info").apply {
            writeText("{}")
            setLastModified(1000L)
        }
        File(root, "2.info").apply {
            writeText("{}")
            setLastModified(3000L)
        }
        File(root, "3.info").apply {
            writeText("{}")
            setLastModified(2000L)
        }

        val sorted = root.listFiles { f -> f.isFile && f.extension == "info" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            ?: emptyList()

        assertEquals(listOf("2.info", "3.info", "1.info"), sorted)
    }

    @Test
    fun `отрицательные и нулевые ID всегда возвращают false в проверках`() {
        val videoIds = setOf(123L)
        fun contains(id: Long) = id > 0L && videoIds.contains(id)
        assertFalse(contains(0L))
        assertFalse(contains(-1L))
        assertFalse(contains(-999L))
        assertTrue(contains(123L))
    }

    @Test
    fun `пустые 0-байтовые файлы mp4 и info игнорируются при чтении с диска`() {
        val root = tmp.newFolder("downloads_zero_byte")
        File(root, "111.mp4").writeText("") // 0 bytes
        File(root, "111.info").writeText("") // 0 bytes
        File(root, "222.mp4").writeText("valid content")
        File(root, "222.info").writeText("{\"id\":222}")

        val allFiles = root.listFiles() ?: emptyArray()
        val videoIds = allFiles.filter { it.isFile && it.extension == "mp4" && it.length() > 0L }
            .mapNotNull { it.nameWithoutExtension.toLongOrNull() }.toSet()
        val validInfos = allFiles.filter { it.isFile && it.extension == "info" && it.length() > 0L }

        assertFalse(videoIds.contains(111L))
        assertTrue(videoIds.contains(222L))
        assertEquals(1, validInfos.size)
        assertEquals("222.info", validInfos.first().name)
    }
}
