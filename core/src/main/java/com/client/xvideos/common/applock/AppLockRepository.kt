package com.client.xvideos.common.applock

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.core.content.edit
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.util.defaultSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Репозиторий защиты приложения и управления кодом доступа.
 *
 * Хранит хэш пароля (PBKDF2, 120 000 итераций), криптографическую соль и статус
 * блокировки в аппаратно зашифрованном хранилище [AppLockSecureStore] (Android Keystore).
 *
 * Защищает от:
 * - Чтения и модификации SharedPreferences через Root / TWRP / run-as (данные зашифрованы AES-256).
 * - Подделки/сброса статуса блокировки (Anti-tampering: сверка с защищённым флагом в Keystore).
 * - Сброса анти-брутфорс задержки (счётчики попыток скрыты в Keystore).
 * - Офлайн-перебора хэша (хэш и соль недоступны без аппаратного мастер-ключа устройства).
 */
object AppLockRepository {

    private const val KEY_PASSWORD_HASH = "app_lock_password_hash"
    private const val KEY_PASSWORD_SALT = "app_lock_password_salt"
    private const val KEY_SECURE_LOCK_ENABLED = "app_lock_secure_enabled"
    private const val KEY_LEGACY_SETTINGS_ENABLED = "app_lock_enabled"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val HASH_BITS = 256
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val MIN_PASSWORD_LENGTH = 4

    // --- Анти-брутфорс (троттлинг попыток) ---
    private const val KEY_FAILED_ATTEMPTS = "app_lock_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "app_lock_lockout_until"
    private const val KEY_LOCKOUT_UNTIL_ELAPSED = "app_lock_lockout_until_elapsed"

    private val testPrefsRef = AtomicReference<SharedPreferences?>(null)
    private val testLegacyPrefsRef = AtomicReference<SharedPreferences?>(null)
    private val testElapsedRealtimeRef = AtomicReference<(() -> Long)?>(null)

    internal fun setTestPreferencesForTesting(prefs: SharedPreferences?) {
        testPrefsRef.set(prefs)
    }

    internal fun setTestLegacyPreferencesForTesting(prefs: SharedPreferences?) {
        testLegacyPrefsRef.set(prefs)
    }

    internal fun setTestElapsedRealtimeForTesting(provider: (() -> Long)?) {
        testElapsedRealtimeRef.set(provider)
    }

    internal fun resetForTesting() {
        testPrefsRef.set(null)
        testLegacyPrefsRef.set(null)
        testElapsedRealtimeRef.set(null)
    }

    private fun getPrefs(context: Context?): SharedPreferences? {
        testPrefsRef.get()?.let { testPrefs ->
            migrateLegacyIfNeeded(context, testPrefs)
            return testPrefs
        }
        val securePrefs = AppLockSecureStore.getOrNull(context)
        if (securePrefs != null) {
            migrateLegacyIfNeeded(context, securePrefs)
            return securePrefs
        }
        testLegacyPrefsRef.get()?.let { return it }
        return runCatching { context?.applicationContext?.defaultSharedPreferences() }.getOrNull()
    }

    /**
     * Однократная бесшовная миграция данных из открытого SharedPreferences в Keystore.
     */
    private fun migrateLegacyIfNeeded(context: Context?, securePrefs: SharedPreferences) {
        val defaultPrefs = testLegacyPrefsRef.get() ?: runCatching {
            context?.applicationContext?.defaultSharedPreferences()
        }.getOrNull() ?: return

        val legacyHash = defaultPrefs.getString(KEY_PASSWORD_HASH, null) ?: return
        val legacySalt = defaultPrefs.getString(KEY_PASSWORD_SALT, null) ?: return

        val legacyAttempts = defaultPrefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        val legacyLockoutWall = defaultPrefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val legacyLockoutElapsed = defaultPrefs.getLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L)
        val legacyEnabled = defaultPrefs.getBoolean(KEY_LEGACY_SETTINGS_ENABLED, false)

        securePrefs.edit {
            putString(KEY_PASSWORD_HASH, legacyHash)
            putString(KEY_PASSWORD_SALT, legacySalt)
            putBoolean(KEY_SECURE_LOCK_ENABLED, legacyEnabled)
            if (legacyAttempts > 0) putInt(KEY_FAILED_ATTEMPTS, legacyAttempts)
            if (legacyLockoutWall > 0L) putLong(KEY_LOCKOUT_UNTIL, legacyLockoutWall)
            if (legacyLockoutElapsed > 0L) putLong(KEY_LOCKOUT_UNTIL_ELAPSED, legacyLockoutElapsed)
        }

        // Затираем открытые секреты из обычного SharedPreferences файла
        defaultPrefs.edit {
            remove(KEY_PASSWORD_HASH)
            remove(KEY_PASSWORD_SALT)
            remove(KEY_FAILED_ATTEMPTS)
            remove(KEY_LOCKOUT_UNTIL)
            remove(KEY_LOCKOUT_UNTIL_ELAPSED)
        }
        Timber.i("AppLockRepository: открытые параметры замка успешно мигрированы в Keystore-хранилище")
    }

    fun isPasswordSet(context: Context? = null): Boolean {
        val prefs = getPrefs(context) ?: return false
        val hash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PASSWORD_SALT, null) ?: return false
        return hash.isNotBlank() && salt.isNotBlank() && hash.fromBase64() != null && salt.fromBase64() != null
    }

    /**
     * Проверяет, включена ли защита приложения.
     *
     * Содержит защиту от несанкционированного изменения (tamper resistance): если злоумышленник
     * с правами Root / через TWRP сменил флаг `app_lock_enabled` в незашифрованном XML-файле
     * настроек на `false`, но в Keystore-хранилище замок активен и пароль задан, приложение
     * детектирует вмешательство, восстанавливает настройку и НЕ снимает экран блокировки.
     */
    fun isEnabled(context: Context? = null): Boolean {
        val settingsInitialized = Settings.isInitialized
        val settingsLockEnabled = settingsInitialized && Settings.app_lock_enabled.field.value

        if (!isPasswordSet(context)) return false

        val prefs = getPrefs(context) ?: return settingsLockEnabled
        val secureLockEnabled = prefs.getBoolean(KEY_SECURE_LOCK_ENABLED, false)

        if (secureLockEnabled && !settingsLockEnabled) {
            Timber.w("AppLockRepository: обнаружено стороннее изменение app_lock_enabled в XML (tampering), восстанавливаю защиту")
            if (settingsInitialized) {
                Settings.app_lock_enabled.setValue(true)
            }
            return true
        }

        return secureLockEnabled || settingsLockEnabled
    }

    /**
     * Проверяет, заблокировано ли приложение в текущий момент с учётом таймаута нахождения в фоне.
     * Если защита включена и сессия не активна (или истёк фоновый таймаут), возвращает `true`.
     */
    fun isAppLocked(context: Context? = null): Boolean {
        if (!isEnabled(context)) return false
        val timeoutSeconds = if (Settings.isInitialized) {
            Settings.app_lock_timeout_seconds.field.value
        } else {
            AppLockTimeout.DEFAULT.seconds
        }
        val elapsedNow = currentElapsedRealtime()
        return !AppLockSession.checkSessionValid(
            timeoutSeconds = timeoutSeconds,
            isLockConfigured = true,
            elapsedNow = elapsedNow
        )
    }

    fun shouldShowLock(context: Context? = null): Boolean {
        return isAppLocked(context)
    }

    /**
     * Подбор хеша стоит [ITERATIONS] итераций PBKDF2 — это сотни миллисекунд, и
     * держать их на главном потоке нельзя: экран замка показывается при каждом
     * запуске, и каждая попытка ввода замораживала интерфейс. Отсюда `suspend` и
     * [Dispatchers.Default] — считаем на процессоре, а не в UI.
     */
    suspend fun setPassword(context: Context? = null, password: String): Result<Unit> = runCatching {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Код доступа должен быть не короче $MIN_PASSWORD_LENGTH символов"
        }

        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = withContext(Dispatchers.Default) { hashPassword(password, salt) }
        val prefs = getPrefs(context) ?: error("Хранилище настроек недоступно для сохранения пароля")

        prefs.edit {
            putString(KEY_PASSWORD_SALT, salt.toBase64())
            putString(KEY_PASSWORD_HASH, hash.toBase64())
            putBoolean(KEY_SECURE_LOCK_ENABLED, true)
        }
        if (Settings.isInitialized) {
            Settings.app_lock_enabled.setValue(true)
        }
        AppLockSession.unlock()
        resetFailedAttempts(context)
    }

    /** См. [setPassword]: считает столько же, поэтому тоже не на главном потоке. */
    suspend fun verifyPassword(context: Context? = null, password: String): Boolean {
        val prefs = getPrefs(context) ?: return false
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
    fun lockoutRemainingMillis(
        context: Context? = null,
        wallNow: Long = System.currentTimeMillis(),
        elapsedNow: Long = currentElapsedRealtime()
    ): Long {
        val prefs = getPrefs(context) ?: return 0L
        val state = AppLockThrottle.State(
            attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            lockoutUntilWall = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L),
            lockoutUntilElapsed = prefs.getLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L),
        )
        return AppLockThrottle.remainingMillis(
            state = state,
            wallNow = wallNow,
            elapsedNow = elapsedNow,
        )
    }

    /**
     * Регистрирует неудачную попытку и возвращает epoch-millis, до которого ввод
     * заблокирован (0 — блокировки пока нет). Задержка растёт экспоненциально
     * после [AppLockThrottle.FREE_ATTEMPTS] ошибок и ограничена
     * [AppLockThrottle.MAX_LOCKOUT_MS].
     */
    fun registerFailedAttempt(
        context: Context? = null,
        wallNow: Long = System.currentTimeMillis(),
        elapsedNow: Long = currentElapsedRealtime()
    ): Long {
        val prefs = getPrefs(context) ?: return 0L
        val state = AppLockThrottle.registerFailure(
            state = AppLockThrottle.State(
                attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
                lockoutUntilWall = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L),
                lockoutUntilElapsed = prefs.getLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L),
            ),
            wallNow = wallNow,
            elapsedNow = elapsedNow,
        )
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, state.attempts)
            putLong(KEY_LOCKOUT_UNTIL, state.lockoutUntilWall)
            putLong(KEY_LOCKOUT_UNTIL_ELAPSED, state.lockoutUntilElapsed)
        }
        return lockoutRemainingMillis(context = context, wallNow = wallNow, elapsedNow = elapsedNow)
    }

    /** Сбрасывает счётчик попыток и блокировку (вызывать после успешного ввода). */
    fun resetFailedAttempts(context: Context? = null) {
        val prefs = getPrefs(context) ?: return
        prefs.edit {
            remove(KEY_FAILED_ATTEMPTS)
            remove(KEY_LOCKOUT_UNTIL)
            remove(KEY_LOCKOUT_UNTIL_ELAPSED)
        }
    }

    fun clearPassword(context: Context? = null) {
        val prefs = getPrefs(context)
        prefs?.edit {
            remove(KEY_PASSWORD_SALT)
            remove(KEY_PASSWORD_HASH)
            putBoolean(KEY_SECURE_LOCK_ENABLED, false)
        }
        if (Settings.isInitialized) {
            Settings.app_lock_enabled.setValue(false)
        }
        AppLockSession.lock()
        resetFailedAttempts(context)
    }

    private fun currentElapsedRealtime(): Long {
        testElapsedRealtimeRef.get()?.let { return it.invoke() }
        return try {
            SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
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
        return Base64.getEncoder().encodeToString(this)
    }

    private fun String.fromBase64(): ByteArray? {
        return runCatching { Base64.getDecoder().decode(this) }.getOrNull()
    }
}
