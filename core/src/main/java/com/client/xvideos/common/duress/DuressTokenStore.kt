package com.client.xvideos.common.duress

import android.content.Context
import android.os.SystemClock
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

/**
 * Одноразовый криптографический токен пропуска экрана замка после ввода кода уничтожения.
 *
 * Требования:
 * - Одноразовый: файл токена удаляется в самом начале проверки.
 * - Короткоживущий: годен не дольше [TOKEN_MAX_AGE_MS] (15 секунд).
 * - Защищён от внешних intent'ов: токен генерируется случайно (128 бит) и хранится
 *   в закрытой памяти приложения (`dataDir`).
 * - Не срабатывает повторно после перезагрузки: отслеживается через [SystemClock.elapsedRealtime].
 */
object DuressTokenStore {

    const val EXTRA_DURESS_TOKEN = "com.client.xvideos.extra.DURESS_TOKEN"
    const val TOKEN_FILE_NAME = ".duress_token"
    const val TOKEN_MAX_AGE_MS = 15_000L
    private const val TOKEN_BYTES = 16

    private val testElapsedRealtimeRef = AtomicReference<(() -> Long)?>(null)
    private val testWallTimeRef = AtomicReference<(() -> Long)?>(null)

    internal fun setTimeProvidersForTesting(
        elapsedProvider: (() -> Long)?,
        wallProvider: (() -> Long)?
    ) {
        testElapsedRealtimeRef.set(elapsedProvider)
        testWallTimeRef.set(wallProvider)
    }

    internal fun resetForTesting() {
        testElapsedRealtimeRef.set(null)
        testWallTimeRef.set(null)
    }

    private fun currentElapsedRealtime(): Long {
        testElapsedRealtimeRef.get()?.let { return it.invoke() }
        return try {
            SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

    private fun currentWallTime(): Long {
        testWallTimeRef.get()?.let { return it.invoke() }
        return System.currentTimeMillis()
    }

    /**
     * Генерирует одноразовый токен и записывает его в приватную директорию [dataDir].
     */
    fun createOneTimeToken(dataDir: File): String {
        val bytes = ByteArray(TOKEN_BYTES).also { SecureRandom().nextBytes(it) }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val elapsed = currentElapsedRealtime()
        val wall = currentWallTime()

        val tokenFile = File(dataDir, TOKEN_FILE_NAME)
        runCatching {
            tokenFile.writeText("$token\n$elapsed\n$wall", Charsets.UTF_8)
        }.onFailure {
            Timber.w(it, "DuressTokenStore: не удалось записать токен уничтожения")
        }
        return token
    }

    fun createOneTimeToken(context: Context): String {
        return createOneTimeToken(context.dataDir)
    }

    /**
     * Проверяет токен, переданный из Intent.
     *
     * Файл токена удаляется немедленно, гарантируя одноразовость.
     */
    fun consumeTokenIfValid(dataDir: File, tokenFromIntent: String?): Boolean {
        val tokenFile = File(dataDir, TOKEN_FILE_NAME)
        if (!tokenFile.exists()) return false

        val content = runCatching { tokenFile.readText(Charsets.UTF_8) }.getOrNull()
        // Одноразовость: удаляем файл независимо от исхода дальнейших проверок
        tokenFile.delete()

        if (content.isNullOrBlank() || tokenFromIntent.isNullOrBlank()) return false

        val lines = content.lines()
        if (lines.size < 3) return false

        val storedToken = lines[0].trim()
        val storedElapsed = lines[1].trim().toLongOrNull() ?: return false
        val storedWall = lines[2].trim().toLongOrNull() ?: return false

        // Сверка токена в постоянное время
        if (!MessageDigest.isEqual(storedToken.toByteArray(Charsets.UTF_8), tokenFromIntent.toByteArray(Charsets.UTF_8))) {
            return false
        }

        // Проверка возраста по монотонным и настенным часам
        val elapsedAge = currentElapsedRealtime() - storedElapsed
        val wallAge = currentWallTime() - storedWall

        if (elapsedAge !in 0..TOKEN_MAX_AGE_MS || wallAge !in 0..TOKEN_MAX_AGE_MS) {
            Timber.w("DuressTokenStore: токен уничтожения просрочен (elapsedAge=$elapsedAge, wallAge=$wallAge)")
            return false
        }

        return true
    }

    fun consumeTokenIfValid(context: Context, tokenFromIntent: String?): Boolean {
        return consumeTokenIfValid(context.dataDir, tokenFromIntent)
    }
}
