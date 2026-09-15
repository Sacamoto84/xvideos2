package com.client.xvideos.common.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

class XlrChunkedCryptoTest {

    @Test
    fun `detectType распознает XLR, ZIP и неподдерживаемые форматы`() {
        assertEquals(
            XlrBackupType.ENCRYPTED_XLR,
            XlrChunkedCrypto.detectType(byteArrayOf('X'.code.toByte(), 'L'.code.toByte(), 'R'.code.toByte(), 'B'.code.toByte(), 1, 2))
        )
        assertEquals(
            XlrBackupType.LEGACY_ZIP,
            XlrChunkedCrypto.detectType(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0))
        )
        assertEquals(
            XlrBackupType.UNSUPPORTED,
            XlrChunkedCrypto.detectType(byteArrayOf(1, 2, 3, 4))
        )
        assertEquals(
            XlrBackupType.UNSUPPORTED,
            XlrChunkedCrypto.detectType(byteArrayOf(1, 2))
        )
    }

    @Test
    fun `шифрование и расшифровка пустых данных`() {
        val password = "test-password-123".toCharArray()
        val original = ByteArray(0)

        val out = ByteArrayOutputStream()
        XlrEncryptedOutputStream(out, password).use {
            it.write(original)
        }

        val encryptedBytes = out.toByteArray()
        assertTrue("Зашифрованный файл должен содержать как минимум заголовок", encryptedBytes.size > XlrChunkedCrypto.HEADER_LENGTH)

        val input = ByteArrayInputStream(encryptedBytes)
        val decrypted = XlrEncryptedInputStream(input, password).use {
            it.readBytes()
        }

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `шифрование и расшифровка небольших данных`() {
        val password = "secret_pass_2026".toCharArray()
        val original = "Тестовые данные для проверки шифрования XLR бэкапа 12345!@#$".toByteArray(Charsets.UTF_8)

        val out = ByteArrayOutputStream()
        XlrEncryptedOutputStream(out, password).use {
            it.write(original)
        }

        val decrypted = XlrEncryptedInputStream(ByteArrayInputStream(out.toByteArray()), password).use {
            it.readBytes()
        }

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `шифрование и расшифровка данных на несколько чанков`() {
        val password = "multi_chunk_password".toCharArray()
        // 150 КБ данных (больше 2 чанков по 64 КБ)
        val random = Random(42)
        val original = ByteArray(150 * 1024).also { random.nextBytes(it) }

        val out = ByteArrayOutputStream()
        XlrEncryptedOutputStream(out, password).use {
            it.write(original)
        }

        val decrypted = XlrEncryptedInputStream(ByteArrayInputStream(out.toByteArray()), password).use {
            it.readBytes()
        }

        assertArrayEquals(original, decrypted)
    }

    @Test
    fun `неверный пароль вызывает XlrInvalidPasswordException`() {
        val password = "correct_password".toCharArray()
        val wrongPassword = "wrong_password".toCharArray()
        val original = "Секретный текст".toByteArray(Charsets.UTF_8)

        val out = ByteArrayOutputStream()
        XlrEncryptedOutputStream(out, password).use {
            it.write(original)
        }

        assertThrows(XlrInvalidPasswordException::class.java) {
            XlrEncryptedInputStream(ByteArrayInputStream(out.toByteArray()), wrongPassword).use {
                it.readBytes()
            }
        }
    }

    @Test
    fun `повреждение шифротекста вызывает ошибку целостности`() {
        val password = "integrity_test_password".toCharArray()
        val original = "Очень важные данные".toByteArray(Charsets.UTF_8)

        val out = ByteArrayOutputStream()
        XlrEncryptedOutputStream(out, password).use {
            it.write(original)
        }

        val corrupted = out.toByteArray()
        // Инвертируем байт в теле шифротекста (после заголовка и метаданных чанка)
        val byteToCorrupt = XlrChunkedCrypto.HEADER_LENGTH + 6
        corrupted[byteToCorrupt] = (corrupted[byteToCorrupt].toInt() xor 0xFF).toByte()

        assertThrows(Exception::class.java) {
            XlrEncryptedInputStream(ByteArrayInputStream(corrupted), password).use {
                it.readBytes()
            }
        }
    }

    @Test
    fun `обрезанный поток чанков вызывает XlrCorruptedBackupException`() {
        val password = "truncation_password".toCharArray()
        // Создаем данные на 2 чанка (например, 70 КБ)
        val original = ByteArray(70 * 1024)

        val out = ByteArrayOutputStream()
        XlrEncryptedOutputStream(out, password).use {
            it.write(original)
        }

        val fullData = out.toByteArray()
        // Отрезаем второй чанк целиком — первый чанк имел isLast = false
        // Header (25) + isLast (1) + cipherLen (4) + (64K + 16 tag)
        val truncatedLength = XlrChunkedCrypto.HEADER_LENGTH + 1 + 4 + (64 * 1024 + XlrChunkedCrypto.GCM_TAG_LENGTH_BYTES)
        val truncatedData = fullData.copyOf(truncatedLength)

        assertThrows(XlrCorruptedBackupException::class.java) {
            XlrEncryptedInputStream(ByteArrayInputStream(truncatedData), password).use {
                it.readBytes()
            }
        }
    }

    @Test
    fun `битый заголовок закрывает нижележащий поток и бросает XlrCorruptedBackupException`() {
        var closed = false
        val badInput = object : ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)) {
            override fun close() {
                super.close()
                closed = true
            }
        }

        assertThrows(XlrCorruptedBackupException::class.java) {
            XlrEncryptedInputStream(badInput, "pass".toCharArray())
        }
        assertTrue("Поток данных должен быть закрыт при ошибке заголовка", closed)
    }
}
