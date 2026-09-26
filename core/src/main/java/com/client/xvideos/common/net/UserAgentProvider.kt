package com.client.xvideos.common.net

import kotlin.random.Random

/**
 * Провайдер пользовательских заголовков User-Agent браузеров рабочего стола (Desktop).
 *
 * Используется сетевыми клиентами, парсерами и загрузчиком для обхода ограничений
 * и блокировок мобильных клиентов сторонними CDN-серверами.
 */
object UserAgentProvider {

    private val desktopBrowsers = arrayOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.4 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0"
    )

    /** Основной User-Agent по умолчанию (современный Chrome под Windows 10/11 x64). */
    val defaultUserAgent: String get() = desktopBrowsers[0]

    /** Количество доступных шаблонов User-Agent. */
    val count: Int get() = desktopBrowsers.size

    /** Полный список всех поддерживаемых User-Agent. */
    val allUserAgents: List<String> get() = desktopBrowsers.toList()

    /**
     * Возвращает псевдослучайный User-Agent десктопного браузера для распределения запросов.
     */
    fun randomDesktopBrowser(): String = desktopBrowsers.random(Random.Default)
}
