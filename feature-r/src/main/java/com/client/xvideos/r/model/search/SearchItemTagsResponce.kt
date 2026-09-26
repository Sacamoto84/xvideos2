package com.client.xvideos.r.model.search

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Элемент тега в поисковой выдаче.
 *
 * @property type Тип элемента (по умолчанию "tag").
 * @property text Текст тега.
 * @property gifs Количество гифок с данным тегом.
 */
@Immutable
@Serializable
data class SearchItemTagsResponse(
    @SerialName("type") val type: String = "tag",
    @SerialName("text") val text: String = "",
    @SerialName("gifs") val gifs: Long = 0L
) {
    /** Проверяет валидность названия тега. */
    val isValid: Boolean get() = text.isNotBlank()

    /** Проверяет наличие гифок с этим тегом. */
    val hasGifs: Boolean get() = gifs > 0L

    companion object {
        /** Пустой экземпляр [SearchItemTagsResponse]. */
        val EMPTY = SearchItemTagsResponse()
    }
}
