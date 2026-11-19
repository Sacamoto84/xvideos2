package com.client.xvideos.n.net

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * Состояние проверки Cloudflare Turnstile для раздела nhentai.
 */
object NCloudflareState {

    /**
     * Ссылка для прохождения верификации. Если не null — на экране отображается диалог с WebView.
     */
    private val _challengeUrl = MutableStateFlow<String?>(null)
    val challengeUrl: StateFlow<String?> = _challengeUrl.asStateFlow()

    /**
     * Сохранённое значение куки cf_clearance.
     */
    private val clearanceCookieRef = AtomicReference<String?>(null)
    val cfClearanceCookie: String?
        get() = clearanceCookieRef.get()

    /**
     * Сигнализирует о необходимости открыть WebView для проверки Cloudflare.
     */
    fun triggerChallenge(url: String = "https://nhentai.net/") {
        _challengeUrl.value = url
    }

    /**
     * Успешное завершение проверки Cloudflare: сохраняет полученные куки и скрывает диалог.
     */
    fun onChallengeCompleted(cookieHeader: String?) {
        storeClearance(cookieHeader)
        _challengeUrl.value = null
    }

    internal fun storeClearance(cookieHeader: String?) {
        val clearance = NCookieHelper.extractCookieValue(cookieHeader.orEmpty(), "cf_clearance")
        if (clearance.isNotEmpty()) clearanceCookieRef.set(clearance)
    }

    /**
     * Отмена проверки пользователем.
     */
    fun onChallengeDismissed() {
        _challengeUrl.value = null
    }
}
