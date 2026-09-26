package com.client.xvideos.common.net.doh

/**
 * Провайдеры DNS-over-HTTPS (DoH).
 *
 * Все публичные провайдеры используют прямые IP-адреса для zero-bootstrap:
 * резолвер не совершает открытых DNS-запросов к провайдеру связи для определения
 * адреса самого DoH-сервера.
 */
enum class DohProvider(
    val title: String,
    val description: String,
    val primaryEndpoint: String,
    val secondaryEndpoint: String,
    val bootstrapIps: List<String>
) {
    CLOUDFLARE(
        title = "Cloudflare",
        description = "1.1.1.1 — Быстрый глобальный DNS с защитой приватности",
        primaryEndpoint = "https://1.1.1.1/dns-query",
        secondaryEndpoint = "https://1.0.0.1/dns-query",
        bootstrapIps = listOf("1.1.1.1", "1.0.0.1")
    ),
    GOOGLE(
        title = "Google",
        description = "8.8.8.8 — Высокая доступность и надёжность Google",
        primaryEndpoint = "https://8.8.8.8/resolve",
        secondaryEndpoint = "https://8.8.4.4/resolve",
        bootstrapIps = listOf("8.8.8.8", "8.8.4.4")
    ),
    ADGUARD(
        title = "AdGuard",
        description = "94.140.14.14 — Блокировка рекламы, фишинга и трекеров",
        primaryEndpoint = "https://94.140.14.14/resolve",
        secondaryEndpoint = "https://94.140.15.15/resolve",
        bootstrapIps = listOf("94.140.14.14", "94.140.15.15")
    ),
    CUSTOM(
        title = "Пользовательский",
        description = "Собственный DoH-резолвер (NextDNS, Pi-hole и др.)",
        primaryEndpoint = "",
        secondaryEndpoint = "",
        bootstrapIps = emptyList()
    );

    val isCustom: Boolean get() = this == CUSTOM
    val isCloudflare: Boolean get() = this == CLOUDFLARE
    val isGoogle: Boolean get() = this == GOOGLE
    val isAdGuard: Boolean get() = this == ADGUARD

    companion object {
        val DEFAULT = CLOUDFLARE

        fun fromNameOrDefault(name: String?): DohProvider =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
    }
}
