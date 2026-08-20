package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Строковая настройка для секретов: значение лежит в зашифрованном хранилище
 * (`SecureCredentialStore`), а не в общем файле настроек.
 *
 * Публичный API совпадает с [SettingElementString] — [field] и [setValue], —
 * поэтому места использования не меняются.
 *
 * Если [securePrefs] равен `null` (Keystore недоступен — Compose Preview,
 * экзотическая прошивка), значение живёт только в памяти процесса и на диск не
 * попадает. Пользователю придётся ввести пароль заново после перезапуска, зато
 * секрет гарантированно не окажется в открытом виде.
 *
 * Слушатель `OnSharedPreferenceChangeListener` здесь намеренно не используется:
 * единственная точка записи — [setValue], она же обновляет [field].
 */
class SettingElementSecureString(
    private val securePrefs: SharedPreferences?,
    val name: String,
    val default: String = "",
) {
    private val _field = MutableStateFlow(securePrefs?.getString(name, default) ?: default)
    val field: StateFlow<String> = _field.asStateFlow()

    fun setValue(value: String) {
        securePrefs?.edit { putString(name, value) }
        _field.value = value
    }
}
