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

    /** Проверяет, пуст ли текст тега. */
    val isEmpty: Boolean get() = text.isEmpty()

    /** Проверяет, не пуст ли текст тега. */
    val isNotEmpty: Boolean get() = text.isNotEmpty()

    /** Проверяет наличие непустого текста тега. */
    val hasText: Boolean get() = text.isNotBlank()

    /** Проверяет наличие гифок с этим тегом. */
    val hasGifs: Boolean get() = gifs > 0L

    /** Нормализованный текст тега в нижнем регистре без лишних пробелов. */
    val normalizedText: String get() = text.trim().lowercase()

    /** Проверяет соответствие тега поисковому запросу без учета регистра. */
    fun matches(query: String?): Boolean =
        if (query.isNullOrBlank()) false else text.contains(query.trim(), ignoreCase = true)

    /** Проверяет соответствие поисковому запросу с возвратом true для пустых запросов. */
    fun matchesQuery(query: String?): Boolean =
        if (query.isNullOrBlank()) true else matches(query)

    /** Форматирует количество гифок в компактный вид (k, M). */
    fun formatGifs(): String {
        return when {
            gifs >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", gifs / 1_000_000.0)
            gifs >= 1_000L -> String.format(java.util.Locale.US, "%.1fk", gifs / 1_000.0)
            else -> gifs.toString()
        }
    }

    /** Преобразует элемент выдачи в [com.client.xvideos.r.model.tag.TagInfo]. */
    fun toTagInfo(): com.client.xvideos.r.model.tag.TagInfo =
        com.client.xvideos.r.model.tag.TagInfo(name = text.trim(), count = gifs)

    companion object {
        /** Пустой экземпляр [SearchItemTagsResponse]. */
        val EMPTY = SearchItemTagsResponse()

        /** Создает [SearchItemTagsResponse] из [com.client.xvideos.r.model.tag.TagInfo]. */
        fun fromTagInfo(tagInfo: com.client.xvideos.r.model.tag.TagInfo, type: String = "tag"): SearchItemTagsResponse =
            SearchItemTagsResponse(type = type, text = tagInfo.name, gifs = tagInfo.count)
    }
}
