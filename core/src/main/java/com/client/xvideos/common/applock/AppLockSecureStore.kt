package com.client.xvideos.common.applock

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Зашифрованное хранилище параметров замка приложения на базе Android Keystore.
 *
 * Хранит хэш пароля, криптографическую соль, защищённый статус включения замка и
 * счётчики неудачных попыток ввода. Файл зашифрован алгоритмом AES256-GCM ключом,
 * который генерируется и хранится в аппаратном модуле Android Keystore (TEE / StrongBox)
 * и не может быть извлечён из файловой системы даже при наличии Root-доступа
 * или через TWRP.
 */
@Suppress("DEPRECATION")
object AppLockSecureStore {

    private const val FILE_NAME = "app_lock_secure_prefs"

    private class BuildResult(
        val prefs: SharedPreferences?,
        val isBrokenKeyset: Boolean
    )

    /**
     * Открывает зашифрованное хранилище.
     *
     * Возвращает `null`, если аппаратный Keystore недоступен (например, в Compose Preview,
     * JVM юнит-тестах или на прошивках без Keystore-провайдера).
     */
    fun getOrNull(context: Context?): SharedPreferences? {
        val appContext = runCatching { context?.applicationContext }.getOrNull() ?: return null
        val firstAttempt = build(appContext)
        if (firstAttempt.prefs != null) return firstAttempt.prefs

        if (!firstAttempt.isBrokenKeyset) {
            Timber.w("AppLockSecureStore: Keystore недоступен, $FILE_NAME оставлен как есть")
            return null
        }

        Timber.w("AppLockSecureStore: keyset повреждён, пересоздаю $FILE_NAME")
        runCatching { appContext.deleteSharedPreferences(FILE_NAME) }
        return build(appContext).prefs
    }

    private fun build(appContext: Context): BuildResult = try {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        BuildResult(prefs = prefs, isBrokenKeyset = false)
    } catch (e: GeneralSecurityException) {
        onBuildFailed(e, brokenKeyset = true)
    } catch (e: IOException) {
        onBuildFailed(e, brokenKeyset = true)
    } catch (e: NoClassDefFoundError) {
        // Compose Preview / Robolectric / JVM classpath
        onBuildFailed(e, brokenKeyset = false)
    } catch (e: Throwable) {
        onBuildFailed(e, brokenKeyset = false)
    }

    private fun onBuildFailed(e: Throwable, brokenKeyset: Boolean): BuildResult {
        Timber.e(e, "AppLockSecureStore: EncryptedSharedPreferences недоступны")
        return BuildResult(prefs = null, isBrokenKeyset = brokenKeyset)
    }
}
