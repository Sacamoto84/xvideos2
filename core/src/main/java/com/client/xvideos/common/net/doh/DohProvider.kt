package com.client.xvideos.common.net.doh

/**
 * Провайдеры DNS-over-HTTPS (DoH).
 *
 * Все публичные провайдеры используют прямые IP-адреса для zero-bootstrap:
 * резолвер не совершает открытых DNS-запросов к провайдеру связи для определения
 * адреса самого DoH-сервера.
 *
 * @property title Отображаемое название провайдера.
 * @property description Подробное описание и IP-адреса.
 * @property primaryEndpoint Первичный URL-эндпоинт DoH JSON API.
 * @property secondaryEndpoint Резервный URL-эндпоинт при отказе первичного.
 * @property bootstrapIps Прямые IP-адреса для подключения без DNS-запроса.
 */
enum class DohProvider(
    val title: String,
    val description: String,
    val primaryEndpoint: String,
    val secondaryEndpoint: String,
    val bootstrapIps: List<String>
) {
    /** Провайдер Cloudflare (1.1.1.1). */
    CLOUDFLARE(
        title = "Cloudflare",
        description = "1.1.1.1 — Быстрый глобальный DNS с защитой приватности",
        primaryEndpoint = "https://1.1.1.1/dns-query",
        secondaryEndpoint = "https://1.0.0.1/dns-query",
        bootstrapIps = listOf("1.1.1.1", "1.0.0.1")
    ),

    /** Провайдер Google Public DNS (8.8.8.8). */
    GOOGLE(
        title = "Google",
        description = "8.8.8.8 — Высокая доступность и надёжность Google",
        primaryEndpoint = "https://8.8.8.8/resolve",
        secondaryEndpoint = "https://8.8.4.4/resolve",
        bootstrapIps = listOf("8.8.8.8", "8.8.4.4")
    ),

    /** Провайдер AdGuard DNS (94.140.14.14) с фильтрацией трекеров. */
    ADGUARD(
        title = "AdGuard",
        description = "94.140.14.14 — Блокировка рекламы, фишинга и трекеров",
        primaryEndpoint = "https://94.140.14.14/resolve",
        secondaryEndpoint = "https://94.140.15.15/resolve",
        bootstrapIps = listOf("94.140.14.14", "94.140.15.15")
    ),

    /** Пользовательский кастомный эндпоинт (настраивается в Settings). */
    CUSTOM(
        title = "Пользовательский",
        description = "Собственный DoH-резолвер (NextDNS, Pi-hole и др.)",
        primaryEndpoint = "",
        secondaryEndpoint = "",
        bootstrapIps = emptyList()
    );

    /** Истина, если выбран пользовательский сервер. */
    val isCustom: Boolean get() = this == CUSTOM

    /** Истина, если выбран один из встроенных предустановленных провайдеров. */
    val isPreset: Boolean get() = !isCustom

    /** Истина, если выбран провайдер Cloudflare. */
    val isCloudflare: Boolean get() = this == CLOUDFLARE

    /** Истина, если выбран провайдер Google. */
    val isGoogle: Boolean get() = this == GOOGLE

    /** Истина, если выбран провайдер AdGuard. */
    val isAdGuard: Boolean get() = this == ADGUARD

    /** Истина, если провайдер содержит bootstrap IP-адреса. */
    val hasBootstrapIps: Boolean get() = bootstrapIps.isNotEmpty()

    /** Переход к следующему провайдеру циклически. */
    fun next(): DohProvider {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущему провайдеру циклически. */
    fun prev(): DohProvider {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        /** Провайдер по умолчанию. */
        val DEFAULT = CLOUDFLARE

        /** Список отображаемых названий всех провайдеров. */
        val allTitles: List<String> = entries.map { it.title }

        /** Список строковых имен всех констант перечисления. */
        val allNames: List<String> = entries.map { it.name }

        /**
         * Находит провайдера по строковому имени (без учета регистра) или возвращает null.
         */
        fun fromNameOrNull(name: String?): DohProvider? =
            if (name != null) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        /**
         * Находит провайдера по строковому имени (без учета регистра) или возвращает [DEFAULT].
         */
        fun fromNameOrDefault(name: String?): DohProvider =
            fromNameOrNull(name) ?: DEFAULT

        /**
         * Находит провайдера по порядковому номеру или возвращает [default].
         */
        fun fromOrdinalOrDefault(ordinal: Int, default: DohProvider = DEFAULT): DohProvider =
            entries.getOrNull(ordinal) ?: default
    }
}
