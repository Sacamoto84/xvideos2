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

    /** Количество ниш на текущей странице. */
    val count: Int get() = niches.size

    /** Проверяет наличие ниш в выдаче. */
    val hasNiches: Boolean get() = niches.isNotEmpty()

    /** Проверяет, является ли страница первой. */
    val isFirstPage: Boolean get() = page <= 1L

    /** Проверяет, является ли страница последней. */
    val isLastPage: Boolean get() = pages > 0L && page >= pages

    /** Первая найденная ниша или null. */
    val firstOrNull: SearchItemNichesResponse? get() = niches.firstOrNull()

    /**
     * Поиск ниши по идентификатору (id/slug) без учета регистра.
     */
    fun findByIdOrNull(id: String?): SearchItemNichesResponse? {
        if (id.isNullOrBlank()) return null
        val clean = id.trim()
        return niches.firstOrNull { it.id.equals(clean, ignoreCase = true) }
    }

    /**
     * Фильтрует ниши по поисковому запросу.
     */
    fun filterByQuery(query: String?): List<SearchItemNichesResponse> =
        if (query.isNullOrBlank()) niches else niches.filter { it.matches(query) }

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

    /** Нормализованный ID ниши в нижнем регистре без пробелов. */
    val normalizedId: String get() = id.trim().lowercase()

    /** Проверяет валидность id ниши. */
    val isValid: Boolean get() = id.isNotBlank()

    /** Проверяет наличие отображаемого названия. */
    val hasName: Boolean get() = name.isNotBlank()

    /** Проверяет наличие непустого thumbnail URL. */
    val hasThumbnail: Boolean get() = thumbnail.isNotBlank()

    /** Проверяет, что в нише есть гифки. */
    val hasGifs: Boolean get() = gifs > 0L

    /** Проверяет наличие подписчиков. */
    val hasSubscribers: Boolean get() = subscribers > 0L

    /** Проверяет наличие тегов. */
    val hasTags: Boolean get() = tags.isNotEmpty()

    /** Проверяет наличие предпочтений контента. */
    val hasPreferences: Boolean get() = preferences.isNotEmpty()

    /** Проверяет соответствие ниши поисковому запросу по ID, названию или тегам. */
    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return false
        val q = query.trim()
        return id.contains(q, ignoreCase = true) ||
            name.contains(q, ignoreCase = true) ||
            tags.any { it.contains(q, ignoreCase = true) }
    }

    /** Форматирует число гифок в компактный вид (k, M). */
    fun formatGifs(): String {
        return when {
            gifs >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", gifs / 1_000_000.0)
            gifs >= 1_000L -> String.format(java.util.Locale.US, "%.1fk", gifs / 1_000.0)
            else -> gifs.toString()
        }
    }

    /** Форматирует число подписчиков в компактный вид (k, M). */
    fun formatSubscribers(): String {
        return when {
            subscribers >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", subscribers / 1_000_000.0)
            subscribers >= 1_000L -> String.format(java.util.Locale.US, "%.1fk", subscribers / 1_000.0)
            else -> subscribers.toString()
        }
    }

    companion object {
        /** Пустой экземпляр элемента ниши. */
        val EMPTY = SearchItemNichesResponse()
    }
}
