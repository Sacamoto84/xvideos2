package com.client.xvideos.l.featured.saved

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Юнит-тесты валидации безопасности при сохранении файлов в LMediaPersist.
 */
class LMediaPersistSecurityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mockClient = HttpClient(MockEngine) {
        engine {
            addHandler { respond("dummy", HttpStatusCode.OK) }
        }
    }

    @Test
    fun `lDownloadToFile отклоняет небезопасные URI-схемы`() {
        val target = File(tempFolder.root, "valid.jpg")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                lDownloadToFile(mockClient, "file:///etc/hosts", target)
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                lDownloadToFile(mockClient, "content://media/external/images/media/1", target)
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                lDownloadToFile(mockClient, "javascript:alert(1)", target)
            }
        }
    }

    @Test
    fun `lDownloadToFile отклоняет небезопасные имена файлов с выходом за пределы каталога`() {
        val root = tempFolder.root
        val outsideFile = File(root, "../escape.jpg")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                lDownloadToFile(mockClient, "https://example.com/pic.jpg", outsideFile)
            }
        }
    }

    @Test
    fun `lDownloadToFile отклоняет имена файлов с нуль-байтами`() {
        val root = tempFolder.root
        val invalidFile = File(root, "null\u0000byte.jpg")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                lDownloadToFile(mockClient, "https://example.com/pic.jpg", invalidFile)
            }
        }
    }
}
