package com.client.xvideos.r.model.search

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ сетевого запроса быстрого поиска ниш по текстовому запросу (`/v2/niches/search?query=...`).
 *
 * @property page Номер страницы.
 * @property pages Всего страниц.
 * @property total Общее количество совпадений.
 * @property niches Список найденных ниш [SearchItemNichesResponse].
 */
@Immutable
@Serializable
data class SearchNichesShortResponse(
    @SerialName("page") val page: Long = 0L,
    @SerialName("pages") val pages: Long = 0L,
    @SerialName("total") val total: Long = 0L,
    @SerialName("niches") val niches: List<SearchItemNichesResponse> = emptyList()
) {
    /** Проверяет, пуст ли результат поиска ниш. */
    val isEmpty: Boolean get() = niches.isEmpty()

    /** Проверяет наличие хотя бы одного результата. */
    val isNotEmpty: Boolean get() = niches.isNotEmpty()

    /** Проверяет наличие последующих страниц. */
    val hasMorePages: Boolean get() = page < pages

    /** Количество ниш на текущей странице. */
    val size: Int get() = niches.size

    /** Проверяет, является ли страница первой. */
    val isFirstPage: Boolean get() = page <= 1L

    companion object {
        /** Пустой экземпляр ответа. */
        val EMPTY = SearchNichesShortResponse()
    }
}

/**
 * Элемент ниши в результатах поиска по названию.
 *
 * Пример JSON:
 * ```json
 * {
 *   "id": "real-orgasms",
 *   "name": "Real Orgasms",
 *   "gifs": 206007,
 *   "subscribers": 457411,
 *   "tags": ["Orgasm", "Orgasms", "Post Orgasm", "Real Orgasm"],
 *   "preferences": ["bisexual", "lesbian", "straight"],
 *   "thumbnail": "https://userpic.redgifs.com/niches/thumbnails/orgasms.jpg"
 * }
 * ```
 *
 * @property id Уникальный слаг ниши.
 * @property name Название ниши.
 * @property gifs Количество гифок в нише.
 * @property subscribers Число подписчиков.
 * @property tags Ассоциированные теги.
 * @property preferences Предпочтения/ориентация контента.
 * @property thumbnail URL превью ниши.
 */
@Immutable
@Serializable
data class SearchItemNichesResponse(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("gifs") val gifs: Long = 0L,
    @SerialName("subscribers") val subscribers: Long = 0L,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("preferences") val preferences: List<String> = emptyList(),
    @SerialName("thumbnail") val thumbnail: String = ""
) {
    /** Отображаемое имя ниши или id как fallback. */
    val displayName: String get() = name.ifBlank { id }

    /** Проверяет валидность id ниши. */
    val isValid: Boolean get() = id.isNotBlank()

    /** Проверяет наличие непустого thumbnail URL. */
    val hasThumbnail: Boolean get() = thumbnail.isNotBlank()

    /** Проверяет, что в нише есть гифки. */
    val hasGifs: Boolean get() = gifs > 0L

    /** Проверяет наличие подписчиков. */
    val hasSubscribers: Boolean get() = subscribers > 0L

    /** Проверяет наличие тегов. */
    val hasTags: Boolean get() = tags.isNotEmpty()

    companion object {
        /** Пустой экземпляр элемента ниши. */
        val EMPTY = SearchItemNichesResponse()
    }
}
