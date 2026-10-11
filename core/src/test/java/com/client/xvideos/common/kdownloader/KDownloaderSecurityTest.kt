package com.client.xvideos.common.kdownloader

import com.client.xvideos.common.kdownloader.httpclient.HttpClient
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.kdownloader.utils.getPath
import com.client.xvideos.common.kdownloader.utils.getRedirectedConnectionIfAny
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Тесты безопасности сетевого загрузчика KDownloader.
 *
 * Проверяет защиту от Path Traversal при формировании целевых путей и
 * защиту от SSL Downgrade / небезопасных URI-схем при редиректах.
 */
class KDownloaderSecurityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `getPath корректно формирует путь для безопасного имени файла`() {
        val dir = tempFolder.root.absolutePath
        val path = getPath(dir, "video123.mp4")
        assertEquals(File(dir, "video123.mp4").canonicalPath, path)
    }

    @Test
    fun `getPath отклоняет попытки выхода за пределы директории`() {
        val dir = tempFolder.root.absolutePath
        assertThrows(IllegalArgumentException::class.java) {
            getPath(dir, "../outside.mp4")
        }
        assertThrows(IllegalArgumentException::class.java) {
            getPath(dir, "folder/sub.mp4")
        }
        assertThrows(IllegalArgumentException::class.java) {
            getPath(dir, "..\\escape.mp4")
        }
        assertThrows(IllegalArgumentException::class.java) {
            getPath(dir, "null\u0000byte.mp4")
        }
    }

    @Test
    fun `DownloadRequest Builder отклоняет небезопасные имена файлов`() {
        val dir = tempFolder.root.absolutePath
        assertThrows(IllegalArgumentException::class.java) {
            DownloadRequest.Builder("https://example.com/v.mp4", dir, "../hack.mp4").build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            DownloadRequest.Builder("https://example.com/v.mp4", dir, "sub/dir.mp4").build()
        }
    }

    private class MockRedirectHttpClient(private val locationHeader: String) : HttpClient {
        override fun clone(): HttpClient = this
        override fun connect(req: DownloadRequest) {}
        override fun getResponseCode(): Int = 302
        override fun getInputStream(): InputStream? = null
        override fun getErrorStream(): InputStream? = null
        override fun getContentLength(): Long = 0
        override fun getHeaderFields(): Map<String, List<String>> = emptyMap()
        override fun getResponseHeader(name: String): String =
            if (name == "Location") locationHeader else ""
        override fun close() {}
    }

    @Test
    fun `getRedirectedConnectionIfAny отклоняет даунгрейд с HTTPS на HTTP`() {
        val request = DownloadRequest.Builder(
            "https://secure.example.com/video.mp4",
            tempFolder.root.absolutePath,
            "video.mp4"
        ).build()

        val mockClient = MockRedirectHttpClient("http://insecure.example.com/video.mp4")

        assertThrows(IOException::class.java) {
            getRedirectedConnectionIfAny(mockClient, request)
        }
    }

    @Test
    fun `getRedirectedConnectionIfAny отклоняет редирект на небезопасные схемы`() {
        val request = DownloadRequest.Builder(
            "https://secure.example.com/video.mp4",
            tempFolder.root.absolutePath,
            "video.mp4"
        ).build()

        val mockClient = MockRedirectHttpClient("file:///etc/hosts")

        assertThrows(IOException::class.java) {
            getRedirectedConnectionIfAny(mockClient, request)
        }
    }
}
