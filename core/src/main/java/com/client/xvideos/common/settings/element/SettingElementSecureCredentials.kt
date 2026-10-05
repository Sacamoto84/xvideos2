package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Логин и пароль, прочитанные одним снимком.
 *
 * Не `data class`: `toString()` не должен печатать пароль, а два снимка с
 * одинаковыми значениями и разными номерами — разные записи.
 *
 * @property revision Номер записи: растёт при каждом сохранении, даже когда
 * значения прежние. По значениям «стёрли и ввели то же самое» не видно.
 */
class SavedCredentials(
    val login: String,
    val password: String,
    val revision: Int,
) {
    /** Аккаунт задан, когда есть и логин, и пароль. */
    val isComplete: Boolean get() = login.isNotBlank() && password.isNotBlank()

    override fun toString(): String = "SavedCredentials(revision=$revision)"
}

/**
 * Логин и пароль одного аккаунта: лежат в зашифрованном хранилище
 * (`SecureCredentialStore`), а не в общем файле настроек, и меняются только
 * вместе, одной записью.
 *
 * Раньше это были две независимые настройки с собственными номерами записи.
 * Читатель между двумя записями видел новый логин со старым паролем или
 * профиль с половиной прироста номера: лишний сброс сессии и второй вход.
 * Здесь значения и номер меняются одним присваиванием [field].
 *
 * Если [securePrefs] равен `null` (Keystore недоступен — Compose Preview,
 * экзотическая прошивка), значения живут только в памяти процесса и на диск не
 * попадают. Пользователю придётся ввести их заново после перезапуска, зато
 * секрет гарантированно не окажется в открытом виде.
 *
 * Слушатель `OnSharedPreferenceChangeListener` здесь намеренно не используется:
 * единственная точка записи — [save], она же обновляет [field].
 */
class SettingElementSecureCredentials(
    private val securePrefs: SharedPreferences?,
    private val loginKey: String,
    private val passwordKey: String,
) {
    private val _field = MutableStateFlow(
        SavedCredentials(
            login = securePrefs?.getString(loginKey, null).orEmpty(),
            password = securePrefs?.getString(passwordKey, null).orEmpty(),
            revision = 0,
        )
    )
    val field: StateFlow<SavedCredentials> = _field.asStateFlow()

    /** Сохраняет логин и пароль одной записью хранилища и одним снимком [field]. */
    @Synchronized
    fun save(login: String, password: String) {
        securePrefs?.edit {
            putString(loginKey, login)
            putString(passwordKey, password)
        }
        _field.value = SavedCredentials(login, password, _field.value.revision + 1)
    }

    /** Стирает логин и пароль. */
    fun clear() = save("", "")
}
