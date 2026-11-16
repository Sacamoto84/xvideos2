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
 * Хранит логин пользователя и куку сессии `sessionid`.
 * При наличии сессии клиент nhentai прикрепляет её к запросам,
 * что снимает жесткие ограничения и увеличивает лимиты обращений к REST API v2.
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

    init {
        runCatching {
            val savedCookie = Settings.n_session_cookie.field.value
            val savedUser = Settings.n_profile.field.value.login
            if (savedCookie.isNotBlank()) {
                _sessionCookie.value = savedCookie
                _username.value = savedUser
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
     * Выход из аккаунта nhentai.
     */
    fun logout() {
        Timber.i("NAuthState: выход из аккаунта")
        _username.value = ""
        _sessionCookie.value = ""
        _isLoggedIn.value = false

        runCatching {
            Settings.n_profile.clear()
            Settings.n_session_cookie.setValue("")
        }

        runCatching {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://nhentai.net")
            if (!cookies.isNullOrBlank()) {
                cookies.split(";").forEach { part ->
                    val cookieName = part.substringBefore("=").trim()
                    if (cookieName.equals("sessionid", ignoreCase = true) ||
                        cookieName.equals("csrftoken", ignoreCase = true)
                    ) {
                        cookieManager.setCookie("https://nhentai.net", "$cookieName=; Max-Age=0")
                    }
                }
                cookieManager.flush()
            }
        }
    }
}
