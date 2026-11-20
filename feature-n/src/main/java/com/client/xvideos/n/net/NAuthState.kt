package com.client.xvideos.n.net

import android.webkit.CookieManager
import com.client.xvideos.common.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

internal data class NRequestCredentials(val apiKey: String = "", val sessionCookie: String = "")

/**
 * Состояние авторизации пользователя в nhentai.
 *
 * Хранит логин пользователя, куку сессии `sessionid` и API-ключ.
 * При наличии сессии или API-ключа клиент nhentai прикрепляет их к запросам,
 * что снимает жесткие ограничения и увеличивает лимиты обращений к REST API v2 (с 15 до 30+ req/min).
 */
object NAuthState {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val credentials = AtomicReference<NRequestCredentials?>(null)
    internal val currentCredentials: NRequestCredentials
        get() = credentials.get() ?: readStoredCredentials()

    val currentSessionCookie: String
        get() = currentCredentials.sessionCookie

    val currentApiKey: String
        get() = currentCredentials.apiKey

    init {
        val saved = readStoredCredentials()
        if (saved.sessionCookie.isNotBlank() || saved.apiKey.isNotBlank()) {
            credentials.set(saved)
            val savedUser = runCatching { Settings.n_profile.field.value.login }.getOrDefault("")
            _username.value = savedUser.ifEmpty { if (saved.apiKey.isNotBlank()) "API Key" else "User" }
            _isLoggedIn.value = true
        }
    }

    private fun readStoredCredentials() = NRequestCredentials(
        apiKey = runCatching { Settings.n_api_key.field.value }.getOrDefault(""),
        sessionCookie = runCatching { Settings.n_session_cookie.field.value }.getOrDefault("")
    )

    /**
     * Вызывается при успешном входе через WebView или форму.
     */
    fun onLoginSuccess(login: String, sessionCookie: String, password: String = "") {
        Timber.i("NAuthState: успешный вход пользователя $login")
        credentials.set(NRequestCredentials(sessionCookie = sessionCookie))
        _username.value = login
        _isLoggedIn.value = true

        runCatching {
            Settings.n_profile.save(login, password)
            Settings.n_api_key.setValue("")
            Settings.n_session_cookie.setValue(sessionCookie)
        }
    }

    /**
     * Сохранение сессионной куки или API-ключа, введённого вручную.
     */
    fun onManualAuth(tokenOrCookie: String, username: String = "") {
        val trimmed = tokenOrCookie.trim()
        val isCookie = trimmed.contains("sessionid=") || (trimmed.length == 32 && trimmed.all { it.isLetterOrDigit() })
        val displayName = username.ifBlank { "Пользователь" }

        if (isCookie) {
            val cookieValue = if (trimmed.contains("sessionid=")) {
                trimmed.substringAfter("sessionid=").substringBefore(";")
            } else {
                trimmed
            }
            onLoginSuccess(displayName, cookieValue)
        } else {
            setApiKey(trimmed, displayName)
        }
    }

    fun setApiKey(key: String, displayName: String = "API Key") {
        val apiKey = key.trim()
        credentials.set(NRequestCredentials(apiKey = apiKey))
        _username.value = displayName
        _isLoggedIn.value = true
        runCatching {
            Settings.n_api_key.setValue(apiKey)
            Settings.n_session_cookie.setValue("")
            Settings.n_profile.save(displayName, "")
        }
    }

    /**
     * Выход из аккаунта nhentai.
     */
    fun logout() {
        Timber.i("NAuthState: выход из аккаунта")
        credentials.set(NRequestCredentials())
        _username.value = ""
        _isLoggedIn.value = false

        runCatching {
            Settings.n_profile.clear()
            Settings.n_session_cookie.setValue("")
            Settings.n_api_key.setValue("")
        }

        runCatching {
            val cookieManager = CookieManager.getInstance()
            val cookies = NCookieHelper.getAllCookies(cookieManager)
            if (cookies.isNotBlank()) {
                cookies.split(";").forEach { part ->
                    val cookieName = part.substringBefore("=").trim()
                    if (cookieName.equals("sessionid", ignoreCase = true) ||
                        cookieName.equals("csrftoken", ignoreCase = true)
                    ) {
                        cookieManager.setCookie("https://nhentai.net/", "$cookieName=; Max-Age=0")
                    }
                }
                cookieManager.flush()
            }
        }
    }
}
