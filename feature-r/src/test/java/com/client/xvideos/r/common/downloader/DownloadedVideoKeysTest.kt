package com.client.xvideos.r.common.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadedVideoKeysTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `ключ собирается из креатора и id`() {
        assertEquals("Creator/AbcDef", downloadedVideoKey("Creator", "AbcDef"))
    }

    @Test
    fun `ключи файлов берут креатора из имени папки`() {
        assertEquals("Creator/AbcDef", downloadedVideoKey(File("Creator/AbcDef.mp4")))
        assertEquals("Other/Xyz", downloadedVideoKey(File("Other/Xyz.mp4")))
    }

    @Test
    fun `файл без родительской папки не роняет разбор`() {
        assertEquals("/AbcDef", downloadedVideoKey(File("AbcDef.mp4")))
    }

    @Test
    fun `размеры готовы только для непустых видео и учитывают автора`() {
        val creator = tempDir.newFolder("Creator")
        val other = tempDir.newFolder("Other")
        File(creator, "abc.mp4").writeBytes(ByteArray(20))
        File(other, "abc.mp4").writeBytes(ByteArray(30))
        File(creator, "pending.mp4").createNewFile()
        val info = File(creator, "pending.info").apply { writeText("""{"userName":"Creator","id":"pending"}""") }
        File(creator, "pending.mp4.temp").writeBytes(ByteArray(40))
        File(creator, "pending.jpg").writeBytes(ByteArray(15))

        val scan = readRedDownloads(tempDir.root)

        assertEquals(mapOf("Creator/abc" to 20L, "Other/abc" to 30L), scan.videoSizes)
        assertEquals(scan.videoSizes.keys, scan.videoKeys)
        assertEquals("pending", scan.list.single().id)
        assertEquals(105L + info.length(), scan.totalBytes)
    }

    @Test
    fun `пустой каталог возвращает пустое множество ключей`() {
        val scan = readRedDownloads(tempDir.root)
        assertTrue(scan.videoKeys.isEmpty())
        assertEquals(0L, scan.totalBytes)
    }

    @Test
    fun `файлы ролика для удаления включают остатки оборванной загрузки`() {
        val userDir = File("downloads", "author")

        val names = downloadedItemFiles(userDir, "abc").map { it.name }.sorted()

        assertEquals(listOf("abc.info", "abc.jpg", "abc.jpg.temp", "abc.mp4", "abc.mp4.temp"), names)
    }
}
