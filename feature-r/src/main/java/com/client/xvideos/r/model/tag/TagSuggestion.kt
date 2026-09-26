package com.client.xvideos.r.model.tag

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Подсказка тега при поисковом автодополнении (`/v2/search/suggest?query=...`).
 *
 * @property gifs Количество гифок с данным тегом.
 * @property text Текст подсказки тега.
 * @property type Тип сущности подсказки.
 */
@Immutable
@Serializable
data class TagSuggestion(
    @SerialName("gifs") val gifs: Long = 0L,
    @SerialName("text") val text: String = "",
    @SerialName("type") val type: String = ""
) {
    /** Проверяет валидность текста подсказки. */
    val isValid: Boolean get() = text.isNotBlank()

    /** Проверяет непустоту текста подсказки. */
    val hasText: Boolean get() = text.isNotBlank()

    /** Проверяет наличие гифок по этой подсказке. */
    val hasGifs: Boolean get() = gifs > 0L

    /** Проверяет наличие указанного типа сущности. */
    val hasType: Boolean get() = type.isNotBlank()

    companion object {
        /** Пустой экземпляр подсказки. */
        val EMPTY = TagSuggestion()
    }
}
