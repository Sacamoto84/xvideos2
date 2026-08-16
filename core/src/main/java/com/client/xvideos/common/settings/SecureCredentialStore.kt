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

        // Пересоздаём файл ТОЛЬКО если ключи действительно не подходят к нему.
        // Раньше пересоздание запускал любой провал build(), включая временную
        // недоступность Keystore (direct boot, часть прошивок) — и сохранённый
        // пароль стирался там, где достаточно было вернуть null и попробовать
        // позже.
        if (!lastFailureLooksLikeBrokenKeyset) {
            Timber.w("SecureCredentialStore: Keystore недоступен, $FILE_NAME оставлен как есть")
            return null
        }

        // Повреждённый keyset (сброс ключей Keystore, восстановление из бэкапа,
        // смена биометрии на части прошивок) расшифровать уже нечем — файл можно
        // только пересоздать. Пользователь потеряет сохранённый пароль и введёт
        // его заново; это лучше, чем неработающий вход в раздел L.
        Timber.w("SecureCredentialStore: keyset повреждён, пересоздаю $FILE_NAME")
        appContext.deleteSharedPreferences(FILE_NAME)
        return build(appContext)
    }

    /**
     * Признак того, что последний отказ [build] выглядит как несовпадение
     * ключей с файлом, а не как недоступность Keystore. Хранится полем, а не
     * возвращается, чтобы не менять сигнатуру `SharedPreferences?`.
     */
    @Volatile
    private var lastFailureLooksLikeBrokenKeyset = false

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
        ).also { lastFailureLooksLikeBrokenKeyset = false }
    } catch (e: GeneralSecurityException) {
        // Tink бросает это, когда keyset не расшифровывается имеющимся
        // мастер-ключом — файл и ключи разошлись, чинится только пересозданием.
        onBuildFailed(e, brokenKeyset = true)
    } catch (e: IOException) {
        // Keyset не разбирается: тот же случай, но на уровне формата файла.
        onBuildFailed(e, brokenKeyset = true)
    } catch (e: Exception) {
        // Всё остальное (IllegalStateException, NoClassDefFoundError в Preview,
        // отказ Keystore) означает «сейчас нельзя», а не «файл испорчен».
        onBuildFailed(e, brokenKeyset = false)
    }

    private fun onBuildFailed(e: Exception, brokenKeyset: Boolean): SharedPreferences? {
        lastFailureLooksLikeBrokenKeyset = brokenKeyset
        Timber.e(e, "SecureCredentialStore: EncryptedSharedPreferences недоступны")
        return null
    }
}
