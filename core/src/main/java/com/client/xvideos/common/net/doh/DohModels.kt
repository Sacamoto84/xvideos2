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
) {
    val isSuccess: Boolean get() = status == 0
    val hasAnswers: Boolean get() = answer.isNotEmpty()
    val isEmpty: Boolean get() = answer.isEmpty() && question.isEmpty()
    val isNotEmpty: Boolean get() = !isEmpty

    companion object {
        val EMPTY = DohResponse()
    }
}

@Serializable
data class DohQuestion(
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: Int = 1
) {
    val isValid: Boolean get() = name.isNotBlank()

    companion object {
        val EMPTY = DohQuestion()
    }
}

@Serializable
data class DohAnswer(
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: Int = 1,
    @SerialName("TTL") val ttl: Long = 300,
    @SerialName("data") val data: String = ""
) {
    val isValid: Boolean get() = name.isNotBlank() && data.isNotBlank()
    val isA: Boolean get() = type == 1
    val isAaaa: Boolean get() = type == 28

    companion object {
        val EMPTY = DohAnswer()
    }
}

/**
 * Результат диагностики резолвинга хоста.
 */
data class DohDiagnosticResult(
    val host: String,
    val addresses: List<String>,
    val elapsedMs: Long,
    val providerTitle: String,
    val isDoh: Boolean
) {
    val isSuccess: Boolean get() = addresses.isNotEmpty()
    val count: Int get() = addresses.size
}
