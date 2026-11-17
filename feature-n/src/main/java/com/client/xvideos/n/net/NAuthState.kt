package com.client.xvideos.n.net

import android.webkit.CookieManager
import com.client.xvideos.common.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

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

    private val _sessionCookie = MutableStateFlow("")
    val currentSessionCookie: String
        get() = _sessionCookie.value.ifEmpty {
            runCatching { Settings.n_session_cookie.field.value }.getOrDefault("")
        }

    private val _apiKey = MutableStateFlow("")
    val currentApiKey: String
        get() = _apiKey.value.ifEmpty {
            runCatching { Settings.n_api_key.field.value }.getOrDefault("")
        }

    init {
        runCatching {
            val savedCookie = Settings.n_session_cookie.field.value
            val savedKey = Settings.n_api_key.field.value
            val savedUser = Settings.n_profile.field.value.login
            if (savedCookie.isNotBlank() || savedKey.isNotBlank()) {
                _sessionCookie.value = savedCookie
                _apiKey.value = savedKey
                _username.value = savedUser.ifEmpty { if (savedKey.isNotBlank()) "API Key" else "User" }
                _isLoggedIn.value = true
            }
        }
    }

    /**
     * Вызывается при успешном входе через WebView или форму.
     */
    fun onLoginSuccess(login: String, sessionCookie: String, password: String = "") {
        Timber.i("NAuthState: успешный вход пользователя $login")
        _username.value = login
        _sessionCookie.value = sessionCookie
        _isLoggedIn.value = true

        runCatching {
            Settings.n_profile.save(login, password)
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

        _username.value = displayName
        _isLoggedIn.value = true

        if (isCookie) {
            val cookieValue = if (trimmed.startsWith("sessionid=")) {
                trimmed.substringAfter("sessionid=").substringBefore(";")
            } else {
                trimmed
            }
            _sessionCookie.value = cookieValue
            runCatching {
                Settings.n_profile.save(displayName, "")
                Settings.n_session_cookie.setValue(cookieValue)
            }
        } else {
            _apiKey.value = trimmed
            runCatching {
                Settings.n_profile.save(displayName, "")
                Settings.n_api_key.setValue(trimmed)
            }
        }
    }

    /**
     * Выход из аккаунта nhentai.
     */
    fun logout() {
        Timber.i("NAuthState: выход из аккаунта")
        _username.value = ""
        _sessionCookie.value = ""
        _apiKey.value = ""
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
