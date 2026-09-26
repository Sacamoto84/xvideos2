package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Обертка ответа API RedGifs на запрос информации об одной нише (`/v2/niches/{niche}`).
 *
 * @property niche Детальная информация о нише [NichesInfo].
 */
@Immutable
@Serializable
data class NicheResponse(
    @SerialName("niche") val niche: NichesInfo = NichesInfo.EMPTY
) {
    /** Проверяет валидность полученной ниши. */
    val isValid: Boolean get() = niche.isValid

    companion object {
        /** Пустой экземпляр ответа. */
        val EMPTY = NicheResponse()
    }
}

/**
 * Модель категории/тематического раздела (ниши) в сервисе RedGifs.
 *
 * Содержит счетчики гифок и подписчиков, обложку, аватарку (thumbnail) и правила модерации раздела.
 *
 * Пример JSON из API:
 * ```json
 * {
 *   "cover": "https://userpic.redgifs.com/niches/covers/big-areolas.jpg",
 *   "description": "NSFW GIFs and images featuring women with large areolas.",
 *   "gifs": 29209,
 *   "id": "big-areolas",
 *   "name": "Big Areolas",
 *   "owner": "phpunit",
 *   "subscribers": 77917,
 *   "thumbnail": "https://userpic.redgifs.com/niches/thumbnails/big-areolas.jpg",
 *   "rules": "1. Big Areolas 2. Porn featuring females with large areolas..."
 * }
 * ```
 *
 * @property cover URL широкого фонового баннера ниши.
 * @property description Текстовое описание тематики ниши.
 * @property gifs Общее количество гифок в нише (-1 если неизвестно).
 * @property id Уникальный слаг-идентификатор ниши (например, "legal-teens").
 * @property name Человекочитаемое название ниши ("Legal Teens").
 * @property owner Никнейм владельца/куратора ниши.
 * @property subscribers Число подписчиков ниши.
 * @property thumbnail URL квадратной иконки/превью (обычно 200x200).
 * @property rules Правила публикации контента в данной нише.
 */
@Immutable
@Serializable
data class NichesInfo(
    @SerialName("cover") val cover: String? = null,           //Большая широкая картинка
    @SerialName("description") val description: String = "",
    @SerialName("gifs") val gifs: Long = -1,
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("owner") val owner: String = "",
    @SerialName("subscribers") val subscribers: Long = -1,
    @SerialName("thumbnail") val thumbnail: String = "", //200x200 картинка
    @SerialName("rules") val rules: String? = null,
) {
    /** Проверяет, валиден ли идентификатор ниши (не пуст и не пробельный). */
    val isValid: Boolean get() = id.isNotBlank()

    /** Проверяет, пуст ли идентификатор ниши. */
    val isEmpty: Boolean get() = id.isEmpty()

    /** Проверяет, не пуст ли идентификатор ниши. */
    val isNotEmpty: Boolean get() = id.isNotEmpty()

    /** Отображаемое имя ниши или id как fallback. */
    val displayName: String get() = name.ifBlank { id }

    /** Проверяет наличие картинки обложки. */
    val hasCover: Boolean get() = !cover.isNullOrBlank()

    /** Проверяет наличие иконки/превью. */
    val hasThumbnail: Boolean get() = thumbnail.isNotBlank()

    /** Проверяет наличие описания. */
    val hasDescription: Boolean get() = description.isNotBlank()

    /** Проверяет наличие владельца/куратора. */
    val hasOwner: Boolean get() = owner.isNotBlank()

    /** Проверяет наличие гифок. */
    val hasGifs: Boolean get() = gifs > 0L

    /** Проверяет наличие подписчиков. */
    val hasSubscribers: Boolean get() = subscribers > 0L

    /** Проверяет наличие правил. */
    val hasRules: Boolean get() = !rules.isNullOrBlank()

    /** Проверяет наличие статистики (гифок или подписчиков). */
    val hasStats: Boolean get() = hasGifs || hasSubscribers

    /** Создает копию с обновленной обложкой. */
    fun withCover(newCover: String?): NichesInfo = copy(cover = newCover)

    /** Создает копию с обновленным превью. */
    fun withThumbnail(newThumbnail: String): NichesInfo = copy(thumbnail = newThumbnail)

    /** Форматирует число подписчиков ниши в компактный вид. */
    fun formatSubscribers(): String {
        if (subscribers <= 0L) return "0"
        return when {
            subscribers >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", subscribers / 1_000_000.0)
            subscribers >= 1_000L -> String.format(java.util.Locale.US, "%.1fk", subscribers / 1_000.0)
            else -> subscribers.toString()
        }
    }

    /** Выбирает наилучшую картинку: обложку или иконку. */
    val bestImageUrl: String get() = cover?.takeIf { it.isNotBlank() } ?: thumbnail

    /** Проверяет соответствие ниши поисковому запросу по имени или идентификатору. */
    fun matches(query: String?): Boolean =
        if (query.isNullOrBlank()) false else name.contains(query.trim(), ignoreCase = true) || id.contains(query.trim(), ignoreCase = true)

    /** Нормализованный идентификатор ниши в нижнем регистре. */
    val normalizedId: String get() = id.trim().lowercase()

    /** Проверяет совпадение ниш по идентификатору. */
    fun isSameNiche(other: NichesInfo?): Boolean =
        other != null && isValid && id.equals(other.id, ignoreCase = true)

    companion object {
        /** Пустой экземпляр ниши. */
        val EMPTY = NichesInfo()
    }
}
