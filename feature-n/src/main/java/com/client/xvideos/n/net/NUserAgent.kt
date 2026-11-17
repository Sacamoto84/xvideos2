package com.client.xvideos.n.net

import android.webkit.WebSettings
import com.client.xvideos.common.AppContextHolder
import java.util.concurrent.atomic.AtomicReference

/**
 * Провайдер согласованного мобильного User-Agent для раздела N (сеть и WebView).
 *
 * Возвращает подлинный системный User-Agent движка WebView Android без принудительного
 * удаления маркеров WebView (таких как '; wv' или 'Version/4.0'). Это исключает
 * рассинхронизацию HTTP-заголовка User-Agent с системными Client Hints (Sec-CH-UA),
 * из-за которой Cloudflare Turnstile фиксирует подделку браузера и завершается
 * ошибкой "Сбой проверки".
 */
object NUserAgent {

    private const val FALLBACK_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.0.0 Mobile Safari/537.36"

    private val cachedUserAgent = AtomicReference<String?>(null)

    val current: String
        get() {
            val existing = cachedUserAgent.get()
            if (existing != null) return existing

            val resolved = runCatching {
                val context = AppContextHolder.getOrNull() ?: return@runCatching FALLBACK_USER_AGENT
                WebSettings.getDefaultUserAgent(context)
            }.getOrDefault(FALLBACK_USER_AGENT)

            cachedUserAgent.compareAndSet(null, resolved)
            return resolved
        }

    fun cleanUserAgent(raw: String): String = raw

    fun resetCacheForTest() {
        cachedUserAgent.set(null)
    }
}
