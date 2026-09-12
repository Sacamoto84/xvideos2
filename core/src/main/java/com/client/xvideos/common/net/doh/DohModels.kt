package com.client.xvideos.common.net.doh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель ответа DoH-резолвера согласно спецификации RFC 8427 / Google DNS JSON API.
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
)

@Serializable
data class DohQuestion(
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: Int = 1
)

@Serializable
data class DohAnswer(
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: Int = 1,
    @SerialName("TTL") val ttl: Long = 300,
    @SerialName("data") val data: String = ""
)

/**
 * Результат диагностики резолвинга хоста.
 */
data class DohDiagnosticResult(
    val host: String,
    val addresses: List<String>,
    val elapsedMs: Long,
    val providerTitle: String,
    val isDoh: Boolean
)
