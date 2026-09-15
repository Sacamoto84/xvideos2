package com.client.xvideos.common.backup

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Тип архива бэкапа, определяемый по сигнатуре первых 4 байт.
 */
enum class XlrBackupType {
    ENCRYPTED_XLR,
    LEGACY_ZIP,
    UNSUPPORTED
}

class XlrInvalidPasswordException(
    message: String = "Неверный пароль для расшифровки бэкапа",
    cause: Throwable? = null
) : GeneralSecurityException(message, cause)

class XlrCorruptedBackupException(message: String, cause: Throwable? = null) :
    IOException(message, cause)

/**
 * Потоковое чанковое шифрование и дешифрование бэкапов.
 *
 * Архитектура:
 * - Заголовок (25 байт): Magic (4B: "XLRB"), Версия (1B: 0x01), Salt (16B), Nonce Prefix (4B).
 * - Чанки (по 64 КБ полезной нагрузки):
 *     - isLast: 1 байт (0x00 или 0x01)
 *     - cipherLength: 4 байта Int (размер шифротекста + GCM auth tag)
 *     - ciphertext: cipherLength байт
 * - Nonce (12 байт): Nonce Prefix (4B) + chunkIndex (8B Big-Endian).
 * - AAD (9 байт): chunkIndex (8B Big-Endian) + isLast (1B: 0 или 1).
 *
 * Свойства безопасности:
 * - Уникальный ключ и вектор инициализации на каждый архив.
 * - Привязка AAD защищает от перестановки, удаления чанков и обрезания архива.
 * - Потоковая обработка по 64 КБ предотвращает OutOfMemoryError на многогигабайтных видео.
 */
object XlrChunkedCrypto {

    const val MAGIC_BYTES_INT = 0x584C5242 // "XLRB"
    val MAGIC_BYTES = byteArrayOf('X'.code.toByte(), 'L'.code.toByte(), 'R'.code.toByte(), 'B'.code.toByte())
    val ZIP_MAGIC_BYTES = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // "PK\x03\x04"

    const val CURRENT_VERSION: Byte = 1
    const val SALT_LENGTH = 16
    const val NONCE_PREFIX_LENGTH = 4
    const val HEADER_LENGTH = 4 + 1 + SALT_LENGTH + NONCE_PREFIX_LENGTH // 25 bytes

    const val CHUNK_SIZE = 64 * 1024 // 64 KB
    const val GCM_TAG_LENGTH_BITS = 128
    const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8

    const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256

    private fun matchesMagic(bytes: ByteArray, magic: ByteArray): Boolean {
        if (bytes.size < magic.size) return false
        for (i in magic.indices) {
            if (bytes[i] != magic[i]) return false
        }
        return true
    }

    fun detectType(firstBytes: ByteArray): XlrBackupType {
        return when {
            matchesMagic(firstBytes, MAGIC_BYTES) -> XlrBackupType.ENCRYPTED_XLR
            matchesMagic(firstBytes, ZIP_MAGIC_BYTES) -> XlrBackupType.LEGACY_ZIP
            else -> XlrBackupType.UNSUPPORTED
        }
    }

    fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            val keyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                .generateSecret(spec)
                .encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    fun buildNonce(prefix: ByteArray, chunkIndex: Long): ByteArray {
        return ByteBuffer.allocate(12)
            .put(prefix)
            .putLong(chunkIndex)
            .array()
    }

    fun buildAad(chunkIndex: Long, isLast: Boolean): ByteArray {
        return ByteBuffer.allocate(9)
            .putLong(chunkIndex)
            .put(if (isLast) 1.toByte() else 0.toByte())
            .array()
    }
}

/**
 * Зашифровывающий поток вывода. Принимает данные произвольными блоками,
 * накапливает по 64 КБ, зашифровывает в режиме AES-GCM и пишет во внутренний поток.
 */
class XlrEncryptedOutputStream(
    private val destination: OutputStream,
    password: CharArray
) : OutputStream() {

    private val dataOutput = DataOutputStream(destination)
    private val key: SecretKey
    private val noncePrefix = ByteArray(XlrChunkedCrypto.NONCE_PREFIX_LENGTH)
    private val buffer = ByteArray(XlrChunkedCrypto.CHUNK_SIZE)
    private var bufferOffset = 0
    private var chunkIndex = 0L
    private var closed = false

    // Отложенный полный блок для корректного выставления флага isLast без пустых хвостовых чанков
    private var pendingChunk: ByteArray? = null

    init {
        val salt = ByteArray(XlrChunkedCrypto.SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        SecureRandom().nextBytes(noncePrefix)
        key = XlrChunkedCrypto.deriveKey(password, salt)

        // Запись заголовка: MAGIC (4B) + VERSION (1B) + SALT (16B) + NONCE_PREFIX (4B)
        dataOutput.write(XlrChunkedCrypto.MAGIC_BYTES)
        dataOutput.writeByte(XlrChunkedCrypto.CURRENT_VERSION.toInt())
        dataOutput.write(salt)
        dataOutput.write(noncePrefix)
    }

    override fun write(b: Int) {
        check(!closed) { "Stream is closed" }
        buffer[bufferOffset++] = b.toByte()
        if (bufferOffset == XlrChunkedCrypto.CHUNK_SIZE) {
            pendingChunk?.let { writeEncryptedChunk(it, isLast = false) }
            pendingChunk = buffer.copyOf()
            bufferOffset = 0
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        check(!closed) { "Stream is closed" }
        var currentOff = off
        var remaining = len

        while (remaining > 0) {
            val space = XlrChunkedCrypto.CHUNK_SIZE - bufferOffset
            val toCopy = minOf(space, remaining)
            System.arraycopy(b, currentOff, buffer, bufferOffset, toCopy)
            bufferOffset += toCopy
            currentOff += toCopy
            remaining -= toCopy

            if (bufferOffset == XlrChunkedCrypto.CHUNK_SIZE) {
                // Буфер полон. Если был предыдущий отложенный чанк, теперь точно известно,
                // что он не последний -> сбрасываем его с isLast = false.
                pendingChunk?.let { writeEncryptedChunk(it, isLast = false) }
                pendingChunk = buffer.copyOf()
                bufferOffset = 0
            }
        }
    }

    override fun flush() {
        check(!closed) { "Stream is closed" }
        dataOutput.flush()
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            if (pendingChunk != null) {
                if (bufferOffset > 0) {
                    // Есть отложенный полный чанк и остаток в буфере -> отложенный не последний
                    writeEncryptedChunk(pendingChunk!!, isLast = false)
                    val lastBytes = buffer.copyOf(bufferOffset)
                    writeEncryptedChunk(lastBytes, isLast = true)
                } else {
                    // Остатка нет -> отложенный полный чанк является последним
                    writeEncryptedChunk(pendingChunk!!, isLast = true)
                }
            } else {
                // Поток меньше 64 КБ (или пустой) -> пишем текущий буфер как последний
                val lastBytes = buffer.copyOf(bufferOffset)
                writeEncryptedChunk(lastBytes, isLast = true)
            }
            dataOutput.flush()
        } finally {
            Arrays.fill(buffer, 0.toByte())
            pendingChunk?.let { Arrays.fill(it, 0.toByte()) }
            dataOutput.close()
        }
    }

    private fun writeEncryptedChunk(data: ByteArray, isLast: Boolean) {
        val nonce = XlrChunkedCrypto.buildNonce(noncePrefix, chunkIndex)
        val aad = XlrChunkedCrypto.buildAad(chunkIndex, isLast)

        val cipher = Cipher.getInstance(XlrChunkedCrypto.CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(XlrChunkedCrypto.GCM_TAG_LENGTH_BITS, nonce))
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(data)

        dataOutput.writeByte(if (isLast) 1 else 0)
        dataOutput.writeInt(ciphertext.size)
        dataOutput.write(ciphertext)

        chunkIndex++
    }
}

/**
 * Расшифровывающий поток ввода. Читает заголовок XLRB, проверяет аутентичность чанков и расшифровывает на лету.
 */
class XlrEncryptedInputStream(
    private val source: InputStream,
    password: CharArray
) : InputStream() {

    private val dataInput = DataInputStream(source)
    private val key: SecretKey
    private val noncePrefix = ByteArray(XlrChunkedCrypto.NONCE_PREFIX_LENGTH)
    private var chunkIndex = 0L
    private var wasLastChunkRead = false
    private var closed = false

    private var decryptedChunk: ByteArray? = null
    private var decryptedOffset = 0

    init {
        try {
            // Чтение и проверка заголовка
            val magic = ByteArray(4)
            dataInput.readFully(magic)
            if (!Arrays.equals(magic, XlrChunkedCrypto.MAGIC_BYTES)) {
                throw XlrCorruptedBackupException("Файл не является зашифрованным бэкапом XLR")
            }

            val version = dataInput.readByte()
            if (version != XlrChunkedCrypto.CURRENT_VERSION) {
                throw XlrCorruptedBackupException("Неподдерживаемая версия формата бэкапа: $version")
            }

            val salt = ByteArray(XlrChunkedCrypto.SALT_LENGTH)
            dataInput.readFully(salt)
            dataInput.readFully(noncePrefix)

            key = XlrChunkedCrypto.deriveKey(password, salt)
        } catch (e: Throwable) {
            runCatching { dataInput.close() }
            throw e
        }
    }

    override fun read(): Int {
        check(!closed) { "Stream is closed" }
        while (decryptedChunk == null || decryptedOffset >= decryptedChunk!!.size) {
            if (wasLastChunkRead) return -1
            readNextChunk()
        }
        val chunk = decryptedChunk ?: return -1
        return chunk[decryptedOffset++].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        check(!closed) { "Stream is closed" }
        if (len == 0) return 0

        while (decryptedChunk == null || decryptedOffset >= decryptedChunk!!.size) {
            if (wasLastChunkRead) {
                return -1 // Достигнут конец потока
            }
            readNextChunk()
        }

        val available = decryptedChunk!!.size - decryptedOffset
        val toRead = minOf(len, available)
        System.arraycopy(decryptedChunk!!, decryptedOffset, b, off, toRead)
        decryptedOffset += toRead
        return toRead
    }

    private fun readNextChunk() {
        val isLastByte = try {
            dataInput.readByte()
        } catch (e: EOFException) {
            throw XlrCorruptedBackupException("Архив повреждён: поток оборван до завершающего блока", e)
        }
        val isLast = (isLastByte == 1.toByte())

        val cipherLength = try {
            dataInput.readInt()
        } catch (e: EOFException) {
            throw XlrCorruptedBackupException("Архив повреждён: не удалось прочитать длину чанка", e)
        }

        if (cipherLength < XlrChunkedCrypto.GCM_TAG_LENGTH_BYTES || cipherLength > XlrChunkedCrypto.CHUNK_SIZE + XlrChunkedCrypto.GCM_TAG_LENGTH_BYTES + 1024) {
            throw XlrCorruptedBackupException("Недопустимый размер зашифрованного чанка: $cipherLength")
        }

        val ciphertext = ByteArray(cipherLength)
        try {
            dataInput.readFully(ciphertext)
        } catch (e: EOFException) {
            throw XlrCorruptedBackupException("Архив повреждён: неполный блок шифротекста", e)
        }

        val nonce = XlrChunkedCrypto.buildNonce(noncePrefix, chunkIndex)
        val aad = XlrChunkedCrypto.buildAad(chunkIndex, isLast)

        val cipher = Cipher.getInstance(XlrChunkedCrypto.CIPHER_ALGORITHM)
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(XlrChunkedCrypto.GCM_TAG_LENGTH_BITS, nonce))
            cipher.updateAAD(aad)
            decryptedChunk = cipher.doFinal(ciphertext)
            decryptedOffset = 0
            wasLastChunkRead = isLast
            chunkIndex++
        } catch (e: AEADBadTagException) {
            if (chunkIndex == 0L) {
                throw XlrInvalidPasswordException(cause = e)
            } else {
                throw XlrCorruptedBackupException("Ошибка целостности данных: несовпадение аутентификационного тега в блоке $chunkIndex", e)
            }
        } catch (e: GeneralSecurityException) {
            throw XlrCorruptedBackupException("Криптографическая ошибка: ${e.message}", e)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        decryptedChunk?.let { Arrays.fill(it, 0.toByte()) }
        decryptedChunk = null
        dataInput.close()
    }
}
