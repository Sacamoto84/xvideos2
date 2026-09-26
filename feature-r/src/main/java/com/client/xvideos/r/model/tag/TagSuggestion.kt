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

    /** Проверяет наличие положительного числа гифок. */
    val hasCount: Boolean get() = gifs > 0L

    /** Проверяет наличие указанного типа сущности. */
    val hasType: Boolean get() = type.isNotBlank()

    /** Нормализованный текст подсказки в нижнем регистре без лишних пробелов. */
    val normalizedText: String get() = text.trim().lowercase()

    /** Проверяет соответствие подсказки поисковому запросу. */
    fun matches(query: String?): Boolean =
        if (query.isNullOrBlank()) false else text.contains(query.trim(), ignoreCase = true)

    /** Проверяет соответствие поисковому запросу с возвратом true для пустых запросов. */
    fun matchesQuery(query: String?): Boolean =
        if (query.isNullOrBlank()) true else matches(query)

    /** Является ли подсказка тегом. */
    val isTagType: Boolean get() = type.equals("tag", ignoreCase = true)

    /** Является ли подсказка нишей. */
    val isNicheType: Boolean get() = type.equals("niche", ignoreCase = true)

    /** Является ли подсказка автором/создателем. */
    val isCreatorType: Boolean get() = type.equals("creator", ignoreCase = true) || type.equals("user", ignoreCase = true)

    /** Преобразует подсказку в [TagInfo]. */
    fun toTagInfo(): TagInfo = TagInfo(name = text.trim(), count = gifs)

    companion object {
        /** Пустой экземпляр подсказки. */
        val EMPTY = TagSuggestion()
    }
}
