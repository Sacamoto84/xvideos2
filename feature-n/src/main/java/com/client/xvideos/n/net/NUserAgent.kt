package com.client.xvideos.n.net

import android.webkit.WebSettings
import com.client.xvideos.common.AppContextHolder
import java.util.concurrent.atomic.AtomicReference

/**
 * Провайдер согласованного мобильного User-Agent для раздела N (сеть и WebView).
 *
 * Не подменяет платформу на Desktop Windows, предотвращая сбой проверки
 * Cloudflare Turnstile ("Сбой проверки"), возникающий из-за несовпадения
 * заголовка User-Agent с реальными свойствами navigator (Linux/Android, touch points).
 */
object NUserAgent {

    private const val FALLBACK_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

    private val cachedUserAgent = AtomicReference<String?>(null)

    val current: String
        get() {
            val existing = cachedUserAgent.get()
            if (existing != null) return existing

            val resolved = runCatching {
                val context = AppContextHolder.getOrNull() ?: return@runCatching FALLBACK_USER_AGENT
                val defaultUa = WebSettings.getDefaultUserAgent(context)
                cleanUserAgent(defaultUa)
            }.getOrDefault(FALLBACK_USER_AGENT)

            cachedUserAgent.compareAndSet(null, resolved)
            return resolved
        }

    fun cleanUserAgent(raw: String): String =
        raw.replace("; wv", "")
            .replace(Regex("Version/\\d+\\.\\d+\\s?"), "")
}
