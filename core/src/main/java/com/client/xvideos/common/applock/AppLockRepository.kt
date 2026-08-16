package com.client.xvideos.common.applock

import android.content.Context
import android.os.SystemClock
import com.client.xvideos.common.util.defaultSharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.client.xvideos.common.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private const val KEY_LOCKOUT_UNTIL_ELAPSED = "app_lock_lockout_until_elapsed"

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

    /**
     * Подбор хеша стоит [ITERATIONS] итераций PBKDF2 — это сотни миллисекунд, и
     * держать их на главном потоке нельзя: экран замка показывается при каждом
     * запуске, и каждая попытка ввода замораживала интерфейс. Отсюда `suspend` и
     * [Dispatchers.Default] — считаем на процессоре, а не в UI.
     */
    suspend fun setPassword(context: Context, password: String): Result<Unit> = runCatching {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Код доступа должен быть не короче $MIN_PASSWORD_LENGTH символов"
        }

        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = withContext(Dispatchers.Default) { hashPassword(password, salt) }
        val prefs = context.applicationContext.defaultSharedPreferences()

        prefs.edit {
            putString(KEY_PASSWORD_SALT, salt.toBase64())
            putString(KEY_PASSWORD_HASH, hash.toBase64())
        }
        Settings.app_lock_enabled.setValue(true)
        AppLockSession.unlock()
        resetFailedAttempts(context)
    }

    /** См. [setPassword]: считает столько же, поэтому тоже не на главном потоке. */
    suspend fun verifyPassword(context: Context, password: String): Boolean {
        val prefs = context.applicationContext.defaultSharedPreferences()
        val salt = prefs.getString(KEY_PASSWORD_SALT, null)?.fromBase64() ?: return false
        val expectedHash = prefs.getString(KEY_PASSWORD_HASH, null)?.fromBase64() ?: return false
        val actualHash = withContext(Dispatchers.Default) { hashPassword(password, salt) }
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    /**
     * Сколько миллисекунд осталось до конца блокировки ввода (0 — ввод разрешён).
     * Срок хранится и настенными, и монотонными часами — см. [AppLockThrottle]:
     * перезапуск приложения задержку не сбрасывает, перевод системного времени
     * её не снимает.
     */
    fun lockoutRemainingMillis(context: Context): Long {
        val prefs = context.applicationContext.defaultSharedPreferences()
        val state = AppLockThrottle.State(
            attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            lockoutUntilWall = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L),
            lockoutUntilElapsed = prefs.getLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L),
        )
        return AppLockThrottle.remainingMillis(
            state = state,
            wallNow = System.currentTimeMillis(),
            elapsedNow = SystemClock.elapsedRealtime(),
        )
    }

    /**
     * Регистрирует неудачную попытку и возвращает epoch-millis, до которого ввод
     * заблокирован (0 — блокировки пока нет). Задержка растёт экспоненциально
     * после [AppLockThrottle.FREE_ATTEMPTS] ошибок и ограничена
     * [AppLockThrottle.MAX_LOCKOUT_MS].
     */
    fun registerFailedAttempt(context: Context): Long {
        val prefs = context.applicationContext.defaultSharedPreferences()
        val state = AppLockThrottle.onFailedAttempt(
            attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            wallNow = System.currentTimeMillis(),
            elapsedNow = SystemClock.elapsedRealtime(),
        )
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, state.attempts)
            putLong(KEY_LOCKOUT_UNTIL, state.lockoutUntilWall)
            putLong(KEY_LOCKOUT_UNTIL_ELAPSED, state.lockoutUntilElapsed)
        }
        return state.lockoutUntilWall
    }

    /** Сбрасывает счётчик попыток и блокировку (вызывать после успешного ввода). */
    fun resetFailedAttempts(context: Context) {
        val prefs = context.applicationContext.defaultSharedPreferences()
        prefs.edit {
            remove(KEY_FAILED_ATTEMPTS)
            remove(KEY_LOCKOUT_UNTIL)
            remove(KEY_LOCKOUT_UNTIL_ELAPSED)
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
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            // Затирает копию пароля внутри spec. Сам по себе не панацея — строка
            // с паролем всё равно живёт в пуле, — но копию, которой мы владеем,
            // держать в куче дольше нужного незачем.
            spec.clearPassword()
        }
    }

    private fun ByteArray.toBase64(): String {
        return Base64.encodeToString(this, Base64.NO_WRAP)
    }

    private fun String.fromBase64(): ByteArray {
        return Base64.decode(this, Base64.NO_WRAP)
    }
}
