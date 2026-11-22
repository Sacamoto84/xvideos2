package com.client.xvideos.common.settings.element

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

data class NAccessCredentials(val apiKey: String = "", val sessionCookie: String = "") {
    override fun toString() = "NAccessCredentials"
}

/** Ключ API и сессия меняются одной записью, чтобы запрос видел актуальную пару. */
class NAccessSettings(
    private val legacy: SharedPreferences,
    private val secure: SharedPreferences?,
) {
    private val _field = MutableStateFlow(load())
    val field = _field.asStateFlow()

    /** Возвращает `false`, когда новые данные действуют только до перезапуска. */
    @Synchronized
    fun save(credentials: NAccessCredentials): Boolean {
        _field.value = credentials
        // При временной недоступности Keystore замена или выход не должны
        // вернуть прежнюю сессию при следующем запуске.
        if (secure != null) {
            try {
                if (secure.edit().write(credentials).commit()) {
                    if (hasLegacy() || resetRequested()) removeLegacy()
                    return true
                }
            } catch (failure: SecurityException) {
                Timber.w("N: зашифрованная запись недоступна (%s)", failure.javaClass.simpleName)
            }
        }
        removeLegacy(reset = true)
        return false
    }

    private fun load(): NAccessCredentials {
        val reset = resetRequested()
        val old = if (reset) NAccessCredentials() else legacy.read()
        val saved = secure ?: return old
        try {
            if (!reset && (saved.contains(API_KEY) || saved.contains(SESSION_COOKIE))) {
                val credentials = saved.read()
                if (hasLegacy()) removeLegacy()
                return credentials
            }
            if ((reset || hasLegacy()) && saved.edit().write(old).commit()) removeLegacy()
        } catch (failure: SecurityException) {
            Timber.w("N: зашифрованное чтение недоступно (%s)", failure.javaClass.simpleName)
        }
        return old
    }

    private fun hasLegacy() = legacy.contains(API_KEY) || legacy.contains(SESSION_COOKIE)

    private fun resetRequested() = legacy.getBoolean(RESET_STORED_ACCESS, false)

    private fun removeLegacy(reset: Boolean = false) = legacy.edit {
        remove(API_KEY)
        remove(SESSION_COOKIE)
        if (reset) putBoolean(RESET_STORED_ACCESS, true) else remove(RESET_STORED_ACCESS)
    }

    private fun SharedPreferences.read() = NAccessCredentials(
        getString(API_KEY, null).orEmpty(),
        getString(SESSION_COOKIE, null).orEmpty(),
    )

    private fun SharedPreferences.Editor.write(credentials: NAccessCredentials): SharedPreferences.Editor =
        putString(API_KEY, credentials.apiKey).putString(SESSION_COOKIE, credentials.sessionCookie)

    private companion object {
        const val API_KEY = "n_api_key"
        const val SESSION_COOKIE = "n_session_cookie"
        const val RESET_STORED_ACCESS = "n_reset_stored_access"
    }
}
