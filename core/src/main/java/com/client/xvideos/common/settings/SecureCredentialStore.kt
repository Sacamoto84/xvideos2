package com.client.xvideos.common.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

/**
 * Отдельный файл настроек, зашифрованный ключом из Android Keystore.
 *
 * Нужен для секретов, которым не место в обычных `SharedPreferences`: раньше
 * логин и пароль Luscious лежали в общем файле настроек открытым текстом, хотя
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
    fun createOrNull(context: Context): SharedPreferences? {
        val appContext = context.applicationContext
        build(appContext)?.let { return it }

        // Повреждённый keyset (сброс ключей Keystore, восстановление из бэкапа,
        // смена биометрии на части прошивок) расшифровать уже нечем — файл можно
        // только пересоздать. Пользователь потеряет сохранённый пароль и введёт
        // его заново; это лучше, чем неработающий вход в раздел L.
        Timber.w("SecureCredentialStore: хранилище не открылось, пересоздаю $FILE_NAME")
        appContext.deleteSharedPreferences(FILE_NAME)
        return build(appContext)
    }

    private fun build(appContext: Context): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        Timber.e(e, "SecureCredentialStore: EncryptedSharedPreferences недоступны")
        null
    }
}
