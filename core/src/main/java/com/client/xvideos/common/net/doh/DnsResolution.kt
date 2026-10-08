package com.client.xvideos.common.net.doh

/** Кто на самом деле нашёл имя. */
enum class DnsSource {
    /** Имя уже было адресом — искать нечего. */
    LITERAL,

    /** Системный DNS: DoH выключен либо имя не идёт через DoH (локальное, адрес самого резолвера). */
    SYSTEM,

    /** Ответ резолвера DoH. */
    DOH,

    /** Ответ DoH, полученный раньше и взятый из кэша. */
    DOH_CACHE,

    /** DoH не ответил, имя нашёл системный DNS. */
    SYSTEM_FALLBACK,
}

/**
 * Найденные адреса и то, кто их нашёл.
 *
 * Источник нужен диагностике: раньше она подписывала любой ответ названием
 * выбранного провайдера DoH, хотя отвечать мог системный DNS или кэш.
 */
internal class DnsResolution<T>(val addresses: List<T>, val source: DnsSource)

/**
 * Поиск имени, когда DoH включён: кэш, затем DoH, затем — если разрешено —
 * системный DNS.
 *
 * @param cached Ответ из кэша; `null` — в кэше нет либо кэш читать не нужно.
 * @param fallbackToSystem Разрешён ли откат на системный DNS.
 * @param queryDoh Опрос адресов DoH.
 * @param systemLookup Поиск через системный DNS.
 * @param onUnresolved DoH не ответил, а отката нет; получает ошибку последнего адреса DoH.
 */
internal fun <T> resolveViaDoh(
    cached: List<T>?,
    fallbackToSystem: Boolean,
    queryDoh: () -> DohOutcome<T>,
    systemLookup: () -> List<T>,
    onUnresolved: (cause: Exception?) -> Nothing,
): DnsResolution<T> {
    if (cached != null) return DnsResolution(cached, DnsSource.DOH_CACHE)
    val doh = queryDoh()
    if (doh.addresses.isNotEmpty()) return DnsResolution(doh.addresses, DnsSource.DOH)
    if (!fallbackToSystem) onUnresolved(doh.error)
    return DnsResolution(systemLookup(), DnsSource.SYSTEM_FALLBACK)
}
