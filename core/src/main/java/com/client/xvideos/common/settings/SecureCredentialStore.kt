package com.client.xvideos.common.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Отдельный файл настроек, зашифрованный ключом из Android Keystore.
 *
 * Нужен для секретов, которым не место в обычных `SharedPreferences`: раньше
 * логин и пароль L лежали в общем файле настроек открытым текстом, хотя
 * собственный код-доступ приложение защищает PBKDF2 со 120 000 итераций.
 *
 * Мастер-ключ хранится в Keystore и не покидает устройство. Поскольку в
 * манифесте стоит `android:allowBackup="false"`, зашифрованный файл не попадёт
 * в бэкап и не приедет на другое устройство, где его нечем расшифровать.
 */
// MasterKey и EncryptedSharedPreferences помечены @Deprecated в
// androidx.security:security-crypto 1.1.0: AndroidX свернула библиотеку, замены
// внутри неё нет — предлагается шифровать самим поверх Keystore. Пока это
// единственное, что даёт подключённая зависимость, и оно работает; альтернатива
// (свой слой на KeyStore + Tink) — отдельная задача.
@Suppress("DEPRECATION")
object SecureCredentialStore {

    private const val FILE_NAME = "secure_credentials"

    /**
     * Открывает зашифрованное хранилище.
     *
     * Возвращает `null`, если Keystore недоступен — так бывает в Compose Preview
     * (LayoutLib) и на отдельных прошивках. Вызывающий обязан пережить `null`:
     * лучше не сохранить секрет вообще, чем записать его открытым текстом.
     */
    private class BuildOutcome(
        val prefs: SharedPreferences?,
        val brokenKeyset: Boolean,
    )

    @Synchronized
    fun createOrNull(context: Context): SharedPreferences? {
        val appContext = context.applicationContext
        val attempt = build(appContext)
        attempt.prefs?.let { return it }

        if (!attempt.brokenKeyset) {
            Timber.w("SecureCredentialStore: Keystore недоступен, $FILE_NAME оставлен как есть")
            return null
        }

        Timber.w("SecureCredentialStore: keyset повреждён, пересоздаю $FILE_NAME")
        appContext.deleteSharedPreferences(FILE_NAME)
        return build(appContext).prefs
    }

    private fun build(appContext: Context): BuildOutcome = try {
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
        BuildOutcome(prefs = prefs, brokenKeyset = false)
    } catch (e: GeneralSecurityException) {
        onBuildFailed(e, brokenKeyset = true)
    } catch (e: IOException) {
        onBuildFailed(e, brokenKeyset = true)
    } catch (e: NoClassDefFoundError) {
        onBuildFailed(e, brokenKeyset = false)
    } catch (e: Exception) {
        onBuildFailed(e, brokenKeyset = false)
    }

    private fun onBuildFailed(e: Throwable, brokenKeyset: Boolean): BuildOutcome {
        Timber.e(e, "SecureCredentialStore: EncryptedSharedPreferences недоступны")
        return BuildOutcome(prefs = null, brokenKeyset = brokenKeyset)
    }
}
