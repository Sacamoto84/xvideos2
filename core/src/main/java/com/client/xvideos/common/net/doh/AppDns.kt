package com.client.xvideos.common.net.doh

import com.client.xvideos.common.settings.Settings
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Централизованный DNS-резолвер приложения с поддержкой DNS-over-HTTPS (DoH).
 *
 * Особенности:
 * 1. Zero-Bootstrap: обращается к публичным серверам (Cloudflare 1.1.1.1, Google 8.8.8.8,
 *    AdGuard 94.140.14.14) напрямую по защищённым IP-адресам через TLS, без открытых
 *    DNS-запросов провайдеру связи.
 * 2. In-memory TTL Cache: быстрый доступ за 0 мс к ранее разрешённым адресам
 *    с автоматической инвалидацией по TTL ответа сервера.
 * 3. Dynamic Switch: мгновенно переключается на системный DNS или другого провайдера
 *    при изменении настроек пользователем, без перезапуска приложения.
 * 4. Fallback resilience: при недоступности DoH переключается на системный DNS,
 *    гарантируя бесперебойность связи.
 */
object AppDns : Dns {

    private const val TYPE_A = 1
    private const val TYPE_AAAA = 28
    private const val MIN_TTL_SECONDS = 30L
    private const val MAX_TTL_SECONDS = 3600L
    private const val DOH_TIMEOUT_SECONDS = 5L
    private const val DNS_JSON_MIME = "application/dns-json"
    private const val MAX_CACHE_SIZE = 256

    private val IPV4_REGEX = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Выделенный OkHttpClient для DoH-запросов.
     * Обязательно использует Dns.SYSTEM, чтобы исключить зацикливание вызовов DNS.
     */
    private val dohHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(Dns.SYSTEM)
            .connectTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DOH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private data class CacheEntry(
        val addresses: List<InetAddress>,
        val expiresAtMs: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override fun lookup(hostname: String): List<InetAddress> {
        val trimmed = hostname.trim().trimEnd('.')

        if (trimmed.isEmpty()) {
            throw UnknownHostException("Empty hostname")
        }

        // Нормализация хоста: нижний регистр по RFC 1035 + Punycode (IDN) для интернационализованных доменов
        val cleanHost = runCatching { java.net.IDN.toASCII(trimmed.lowercase()) }.getOrDefault(trimmed.lowercase())

        // Если это уже числовой IP-адрес — сразу возвращаем InetAddress без DNS
        if (isIpAddress(cleanHost)) {
            return listOf(InetAddress.getByName(cleanHost))
        }

        // Локальные адреса не резолвим через публичный DoH
        if (isLocalHost(cleanHost)) {
            return Dns.SYSTEM.lookup(cleanHost)
        }

        val isDohEnabled = runCatching {
            Settings.isInitialized && Settings.doh_enabled.field.value
        }.getOrDefault(false)

        if (!isDohEnabled) {
            return Dns.SYSTEM.lookup(cleanHost)
        }

        // Защита от циклической рекурсии: если запрашивают хост текущего DoH-сервера
        if (isDohServerHost(cleanHost)) {
            return Dns.SYSTEM.lookup(cleanHost)
        }

        // Проверка кэша
        val now = System.currentTimeMillis()
        cache[cleanHost]?.let { entry ->
            if (now < entry.expiresAtMs) {
                return entry.addresses
            }
            cache.remove(cleanHost, entry)
        }

        val fallbackToSystem = runCatching {
            Settings.doh_fallback_to_system.field.value
        }.getOrDefault(true)

        val ipv4Only = runCatching {
            Settings.doh_ipv4_only.field.value
        }.getOrDefault(true)

        val endpoints = getActiveEndpoints()

        var lastException: Exception? = null

        for (endpoint in endpoints) {
            try {
                val addresses = queryDoh(endpoint, cleanHost, ipv4Only, now)
                if (addresses.isNotEmpty()) {
                    return addresses
                }
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "AppDns: DoH запрос к $endpoint для $cleanHost завершился ошибкой")
            }
        }

        if (fallbackToSystem) {
            Timber.i("AppDns: DoH не ответил для $cleanHost, переключаемся на системный DNS (fallback)")
            return Dns.SYSTEM.lookup(cleanHost)
        }

        throw UnknownHostException("Не удалось разрешить хост $cleanHost через DoH: ${lastException?.message}")
            .apply { if (lastException != null) initCause(lastException) }
    }

    private fun queryDoh(
        endpoint: String,
        hostname: String,
        ipv4Only: Boolean,
        now: Long
    ): List<InetAddress> {
        val validAnswers = mutableListOf<DohAnswer>()
        var primaryException: Exception? = null

        try {
            validAnswers.addAll(fetchDohAnswers(endpoint, hostname, "A", TYPE_A))
        } catch (e: Exception) {
            primaryException = e
        }

        if (!ipv4Only) {
            try {
                validAnswers.addAll(fetchDohAnswers(endpoint, hostname, "AAAA", TYPE_AAAA))
            } catch (e: Exception) {
                if (validAnswers.isEmpty() && primaryException == null) {
                    primaryException = e
                } else {
                    Timber.d(e, "AppDns: AAAA DoH запрос к %s для %s не удался", endpoint, hostname)
                }
            }
        }

        if (validAnswers.isEmpty()) {
            if (primaryException != null) {
                throw primaryException
            }
            return emptyList()
        }

        val addresses = validAnswers.mapNotNull { answer ->
            runCatching {
                val rawAddress = InetAddress.getByName(answer.data)
                InetAddress.getByAddress(hostname, rawAddress.address)
            }.getOrNull()
        }

        if (addresses.isNotEmpty()) {
            val minTtl = validAnswers.minOfOrNull { it.ttl } ?: 300L
            val effectiveTtlSeconds = minTtl.coerceIn(MIN_TTL_SECONDS, MAX_TTL_SECONDS)
            val expiresAtMs = now + effectiveTtlSeconds * 1000L
            putInCache(hostname, CacheEntry(addresses, expiresAtMs), now)
        }

        return addresses
    }

    private fun fetchDohAnswers(
        endpoint: String,
        hostname: String,
        typeName: String,
        typeCode: Int
    ): List<DohAnswer> {
        val delimiter = if (endpoint.contains('?')) "&" else "?"
        val queryUrl = "${endpoint}${delimiter}name=${hostname}&type=$typeName"

        val request = Request.Builder()
            .url(queryUrl)
            .header("Accept", DNS_JSON_MIME)
            .build()

        return dohHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DoH HTTP error ${response.code} from $endpoint")
            }
            val body = response.body?.string() ?: throw IOException("Empty body from $endpoint")
            val dohResponse = json.decodeFromString<DohResponse>(body)
            dohResponse.answer.filter { it.type == typeCode && it.data.isNotBlank() }
        }
    }

    private fun getActiveEndpoints(): List<String> {
        val providerName = runCatching { Settings.doh_provider.field.value }.getOrNull()
        val provider = DohProvider.fromNameOrDefault(providerName)

        if (provider == DohProvider.CUSTOM) {
            val customUrl = runCatching { Settings.doh_custom_url.field.value }.getOrDefault("").trim()
            if (customUrl.startsWith("http://") || customUrl.startsWith("https://")) {
                return listOf(customUrl)
            }
            return listOf(DohProvider.CLOUDFLARE.primaryEndpoint, DohProvider.CLOUDFLARE.secondaryEndpoint)
        }

        return listOf(provider.primaryEndpoint, provider.secondaryEndpoint)
    }

    private fun isIpAddress(host: String): Boolean {
        if (host.contains(':')) {
            return runCatching { Inet6Address.getByName(host) is Inet6Address }.getOrDefault(false)
        }
        return host.matches(IPV4_REGEX)
    }

    private fun isLocalHost(host: String): Boolean {
        return host.equals("localhost", ignoreCase = true) ||
            host.endsWith(".local", ignoreCase = true) ||
            host.endsWith(".internal", ignoreCase = true)
    }

    private fun putInCache(hostname: String, entry: CacheEntry, now: Long) {
        if (cache.size >= MAX_CACHE_SIZE) {
            // Очищаем просроченные по TTL записи
            cache.entries.removeIf { now >= it.value.expiresAtMs }
            // Если кэш всё ещё превышает лимит — вытесняем избыток старых записей
            if (cache.size >= MAX_CACHE_SIZE) {
                val excess = cache.size - (MAX_CACHE_SIZE * 3 / 4)
                if (excess > 0) {
                    val it = cache.keys.iterator()
                    var count = 0
                    while (it.hasNext() && count < excess) {
                        it.next()
                        it.remove()
                        count++
                    }
                }
            }
        }
        cache[hostname] = entry
    }

    internal val cacheSize: Int
        get() = cache.size

    internal fun isDohServerHost(host: String): Boolean {
        val providerName = runCatching { Settings.doh_provider.field.value }.getOrNull()
        val provider = DohProvider.fromNameOrDefault(providerName)

        if (provider.bootstrapIps.contains(host)) return true

        if (provider == DohProvider.CUSTOM) {
            val customUrl = runCatching { Settings.doh_custom_url.field.value }.getOrDefault("").trim()
            val customHost = runCatching { java.net.URI(customUrl).host }.getOrNull()
            if (customHost != null && customHost.equals(host, ignoreCase = true)) return true
        }

        return false
    }

    /**
     * Очищает кэшированные DNS-записи.
     */
    fun clearCache() {
        cache.clear()
        Timber.i("AppDns: DNS-кэш успешно очищен")
    }

    /**
     * Диагностический тест резолвинга хоста с замером времени отклика.
     */
    fun diagnose(hostname: String = "api.redgifs.com"): Result<DohDiagnosticResult> {
        val startNs = System.nanoTime()
        return try {
            val isDohEnabled = runCatching {
                Settings.isInitialized && Settings.doh_enabled.field.value
            }.getOrDefault(false)

            val providerTitle = if (isDohEnabled) {
                val providerName = runCatching { Settings.doh_provider.field.value }.getOrNull()
                DohProvider.fromNameOrDefault(providerName).title
            } else {
                "Системный DNS"
            }

            val addresses = lookup(hostname)
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

            Result.success(
                DohDiagnosticResult(
                    host = hostname,
                    addresses = addresses.mapNotNull { it.hostAddress?.takeIf { s -> s.isNotEmpty() } },
                    elapsedMs = elapsedMs,
                    providerTitle = providerTitle,
                    isDoh = isDohEnabled
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
