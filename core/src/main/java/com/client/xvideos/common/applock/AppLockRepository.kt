package com.client.xvideos.common.applock

import android.content.Context
import com.client.xvideos.common.util.defaultSharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.client.xvideos.common.settings.Settings
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppLockRepository {

    private const val KEY_PASSWORD_HASH = "app_lock_password_hash"
    private const val KEY_PASSWORD_SALT = "app_lock_password_salt"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val HASH_BITS = 256
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val MIN_PASSWORD_LENGTH = 4

    // --- Анти-брутфорс (троттлинг попыток) ---
    private const val KEY_FAILED_ATTEMPTS = "app_lock_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "app_lock_lockout_until"

    /** Сколько попыток без задержки до начала блокировки. */
    private const val FREE_ATTEMPTS = 4

    /** Базовая длительность блокировки; далее удваивается на каждую ошибку. */
    private const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 30 * 60_000L // 30 минут
    private const val MAX_BACKOFF_SHIFT = 16

    fun isPasswordSet(context: Context): Boolean {
        val prefs = context.applicationContext.defaultSharedPreferences()
        return !prefs.getString(KEY_PASSWORD_HASH, null).isNullOrBlank() &&
                !prefs.getString(KEY_PASSWORD_SALT, null).isNullOrBlank()
    }

    fun isEnabled(context: Context): Boolean {
        return Settings.app_lock_enabled.field.value && isPasswordSet(context)
    }

    fun shouldShowLock(context: Context): Boolean {
        return isEnabled(context) && !AppLockSession.isUnlocked()
    }

    fun setPassword(context: Context, password: String): Result<Unit> = runCatching {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Код доступа должен быть не короче $MIN_PASSWORD_LENGTH символов"
        }

        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)
        val prefs = context.applicationContext.defaultSharedPreferences()

        prefs.edit {
            putString(KEY_PASSWORD_SALT, salt.toBase64())
            putString(KEY_PASSWORD_HASH, hash.toBase64())
        }
        Settings.app_lock_enabled.setValue(true)
        AppLockSession.unlock()
        resetFailedAttempts(context)
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        val prefs = context.applicationContext.defaultSharedPreferences()
        val salt = prefs.getString(KEY_PASSWORD_SALT, null)?.fromBase64() ?: return false
        val expectedHash = prefs.getString(KEY_PASSWORD_HASH, null)?.fromBase64() ?: return false
        val actualHash = hashPassword(password, salt)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    /**
     * Сколько миллисекунд осталось до конца блокировки ввода (0 — ввод разрешён).
     * Персистентно (переживает перезапуск процесса), поэтому простое убийство
     * приложения не сбрасывает задержку.
     */
    fun lockoutRemainingMillis(context: Context): Long {
        val prefs = context.applicationContext.defaultSharedPreferences()
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    /**
     * Регистрирует неудачную попытку и возвращает epoch-millis, до которого ввод
     * заблокирован (0 — блокировки пока нет). Задержка растёт экспоненциально
     * после [FREE_ATTEMPTS] ошибок и ограничена [MAX_LOCKOUT_MS].
     */
    fun registerFailedAttempt(context: Context): Long {
        val prefs = context.applicationContext.defaultSharedPreferences()
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val lockoutUntil = if (attempts > FREE_ATTEMPTS) {
            val shift = (attempts - FREE_ATTEMPTS - 1).coerceIn(0, MAX_BACKOFF_SHIFT)
            val duration = (BASE_LOCKOUT_MS shl shift).coerceAtMost(MAX_LOCKOUT_MS)
            System.currentTimeMillis() + duration
        } else {
            0L
        }
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, attempts)
            putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
        }
        return lockoutUntil
    }

    /** Сбрасывает счётчик попыток и блокировку (вызывать после успешного ввода). */
    fun resetFailedAttempts(context: Context) {
        val prefs = context.applicationContext.defaultSharedPreferences()
        prefs.edit {
            remove(KEY_FAILED_ATTEMPTS)
            remove(KEY_LOCKOUT_UNTIL)
        }
    }

    fun clearPassword(context: Context) {
        val prefs = context.applicationContext.defaultSharedPreferences()
        prefs.edit {
            remove(KEY_PASSWORD_SALT)
            remove(KEY_PASSWORD_HASH)
        }
        Settings.app_lock_enabled.setValue(false)
        AppLockSession.lock()
        resetFailedAttempts(context)
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_BITS)
        return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
    }

    private fun ByteArray.toBase64(): String {
        return Base64.encodeToString(this, Base64.NO_WRAP)
    }

    private fun String.fromBase64(): ByteArray {
        return Base64.decode(this, Base64.NO_WRAP)
    }
}
