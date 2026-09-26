package com.client.xvideos.common.net.doh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель ответа DoH-резолвера согласно спецификации RFC 8427 / Google DNS JSON API.
 *
 * @property status Код возврата DNS (0 = NOERROR, 3 = NXDOMAIN).
 * @property tc Флаг усечения ответа (Truncated).
 * @property rd Флаг требования рекурсии (Recursion Desired).
 * @property ra Флаг доступности рекурсии (Recursion Available).
 * @property ad Флаг аутентифицированных данных (DNSSEC Authenticated Data).
 * @property cd Флаг отключения проверки DNSSEC (Checking Disabled).
 * @property question Список исходных вопросов DNS-запроса.
 * @property answer Список ресурсных записей ответа (RR).
 */
@Serializable
data class DohResponse(
    @SerialName("Status") val status: Int = 0,
    @SerialName("TC") val tc: Boolean = false,
    @SerialName("RD") val rd: Boolean = false,
    @SerialName("RA") val ra: Boolean = false,
    @SerialName("AD") val ad: Boolean = false,
    @SerialName("CD") val cd: Boolean = false,
    @SerialName("Question") val question: List<DohQuestion> = emptyList(),
    @SerialName("Answer") val answer: List<DohAnswer> = emptyList()
) {
    /** Успешный DNS-статус (0 = NOERROR). */
    val isSuccess: Boolean get() = status == 0

    /** Истина, если DNS-сервер вернул хотя бы одну ресурсную запись. */
    val hasAnswers: Boolean get() = answer.isNotEmpty()

    /** Истина, если ответ пуст (нет ни вопросов, ни ответов). */
    val isEmpty: Boolean get() = answer.isEmpty() && question.isEmpty()

    /** Истина, если ответ содержит данные. */
    val isNotEmpty: Boolean get() = !isEmpty

    /** Возвращает первый найденный IPv4-адрес или null. */
    fun getFirstIpv4OrNull(): String? = answer.firstOrNull { it.isA }?.data

    /** Возвращает первый найденный IPv6-адрес или null. */
    fun getFirstIpv6OrNull(): String? = answer.firstOrNull { it.isAaaa }?.data

    /** Возвращает список всех извлеченных IP-адресов (IPv4 и IPv6). */
    fun allIpAddresses(): List<String> = answer.filter { it.isA || it.isAaaa }.map { it.data }

    companion object {
        /** Пустой объект ответа для fallback-сценариев. */
        val EMPTY = DohResponse()
    }
}

/**
 * Структура DNS-вопроса в DoH JSON-ответе.
 *
 * @property name Имя запрашиваемого домена (например, "example.com").
 * @property type Числовой тип DNS-записи (1 = A, 28 = AAAA).
 */
@Serializable
data class DohQuestion(
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: Int = 1
) {
    /** Проверяет непустоту доменного имени. */
    val isValid: Boolean get() = name.isNotBlank()
    val isA: Boolean get() = type == 1
    val isAaaa: Boolean get() = type == 28

    companion object {
        val EMPTY = DohQuestion()
    }
}

/**
 * Ресурсная запись ответа (Resource Record) в DoH JSON-ответе.
 *
 * @property name Доменное имя записи.
 * @property type Тип ресурсной записи (1 для IPv4 A, 28 для IPv6 AAAA).
 * @property ttl Время жизни записи в секундах (TTL).
 * @property data IP-адрес или каноническое имя (CNAME).
 */
@Serializable
data class DohAnswer(
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: Int = 1,
    @SerialName("TTL") val ttl: Long = 300,
    @SerialName("data") val data: String = ""
) {
    /** Проверяет валидность записи (непустое имя и данные). */
    val isValid: Boolean get() = name.isNotBlank() && data.isNotBlank()

    /** Истина, если запись относится к типу IPv4 (A). */
    val isA: Boolean get() = type == 1

    /** Истина, если запись относится к типу IPv6 (AAAA). */
    val isAaaa: Boolean get() = type == 28

    companion object {
        val EMPTY = DohAnswer()
    }
}

/**
 * Результат диагностики разрешения доменного имени для экрана настроек и тестов.
 *
 * @property host Запрашиваемый хост.
 * @property addresses Список успешно полученных IP-адресов.
 * @property elapsedMs Время выполнения запроса в миллисекундах.
 * @property providerTitle Название использованного провайдера (Cloudflare, Google, AdGuard или System).
 * @property isDoh Флаг использования протокола DNS-over-HTTPS (true) или системного DNS (false).
 */
data class DohDiagnosticResult(
    val host: String,
    val addresses: List<String>,
    val elapsedMs: Long,
    val providerTitle: String,
    val isDoh: Boolean
) {
    /** Истина, если резолвинг завершился успешно и вернул хотя бы один адрес. */
    val isSuccess: Boolean get() = addresses.isNotEmpty()

    /** Количество полученных IP-адресов. */
    val count: Int get() = addresses.size

    val summaryText: String get() = "$providerTitle: ${addresses.size} IPs in ${elapsedMs}ms"
}
