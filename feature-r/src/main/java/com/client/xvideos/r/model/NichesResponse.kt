package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ сетевого запроса списка ниш (каталог раздела Explorer).
 *
 * @property niches Список ниш [Niche] на текущей странице.
 * @property page Номер текущей страницы.
 * @property pages Общее количество страниц.
 * @property total Общее количество ниш в каталоге.
 */
@Immutable
@Serializable
data class NichesResponse(
    @SerialName("niches") val niches: List<Niche> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("pages") val pages: Int = 0,
    @SerialName("total") val total: Int = 0
) {
    /** Проверяет, пуст ли список ниш. */
    val isEmpty: Boolean get() = niches.isEmpty()

    /** Проверяет, содержит ли список хотя бы одну нишу. */
    val isNotEmpty: Boolean get() = niches.isNotEmpty()

    /** Количество ниш в ответе. */
    val count: Int get() = niches.size

    /** Проверяет наличие ниш в ответе. */
    val hasNiches: Boolean get() = niches.isNotEmpty()

    /** Проверяет, является ли страница первой. */
    val isFirstPage: Boolean get() = page <= 1

    /** Проверяет, является ли страница последней. */
    val isLastPage: Boolean get() = pages > 0 && page >= pages

    /** Проверяет наличие последующих страниц в каталоге. */
    val hasMorePages: Boolean get() = page < pages

    companion object {
        /** Пустой экземпляр ответа со значениями по умолчанию. */
        val EMPTY = NichesResponse()
    }
}

/**
 * Краткая модель ниши из каталога Explorer со встроенными превью роликов.
 *
 * @property id Уникальный слаг ниши (например, "female-backs").
 * @property name Название ниши для отображения пользователю.
 * @property gifs Общее количество гифок в нише.
 * @property subscribers Число подписчиков ниши.
 * @property thumbnail URL иконки ниши.
 * @property previews Список превью лучших роликов ниши [Preview].
 */
@Immutable
@Serializable
data class Niche(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("gifs") val gifs: Long = 0L,
    @SerialName("subscribers") val subscribers: Long = 0L,
    @SerialName("thumbnail") val thumbnail: String = "",
    @SerialName("previews") val previews: List<Preview>? = null
) {
    /** Проверяет, валиден ли идентификатор ниши. */
    val isValid: Boolean get() = id.isNotBlank()

    /** Проверяет, пуст ли идентификатор ниши. */
    val isEmpty: Boolean get() = id.isEmpty()

    /** Проверяет, не пуст ли идентификатор ниши. */
    val isNotEmpty: Boolean get() = id.isNotEmpty()

    /** Возвращает отображаемое имя ниши (если name пусто, используется id). */
    val displayName: String get() = name.ifBlank { id }

    /** Проверяет наличие непустого thumbnail URL. */
    val hasThumbnail: Boolean get() = thumbnail.isNotBlank()

    /** Проверяет наличие превью роликов. */
    val hasPreviews: Boolean get() = !previews.isNullOrEmpty()

    /** Проверяет, что в нише есть хотя бы одна гифка. */
    val hasGifs: Boolean get() = gifs > 0L

    /** Проверяет, что у ниши есть хотя бы один подписчик. */
    val hasSubscribers: Boolean get() = subscribers > 0L

    companion object {
        /** Пустой экземпляр [Niche] со значениями по умолчанию. */
        val EMPTY = Niche()
    }
}

/**
 * Модель миниатюры предварительного просмотра для ниши.
 *
 * @property id Идентификатор ролика.
 * @property thumbnail URL картинки превью.
 */
@Immutable
@Serializable
data class Preview(
    @SerialName("id") val id: String = "",
    @SerialName("thumbnail") val thumbnail: String = ""
) {
    /** Проверяет валидность превью (непустые id и thumbnail). */
    val isValid: Boolean get() = id.isNotBlank() && thumbnail.isNotBlank()

    /** Проверяет наличие идентификатора. */
    val hasId: Boolean get() = id.isNotBlank()

    /** Проверяет наличие картинки превью. */
    val hasThumbnail: Boolean get() = thumbnail.isNotBlank()

    companion object {
        /** Пустой экземпляр [Preview]. */
        val EMPTY = Preview()
    }
}
