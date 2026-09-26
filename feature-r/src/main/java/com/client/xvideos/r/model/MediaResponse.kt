package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ сетевого API RedGifs на запросы списков медиаконтента (ленты, поисковая выдача, гифки ниши).
 *
 * Содержит пагинационную информацию и списки сущностей (GIF, авторы, ниши, теги).
 *
 * @property page Текущий номер страницы (1-based).
 * @property pages Общее количество доступных страниц.
 * @property total Общее количество элементов во всей выборке.
 * @property gifs Список медиа-объектов [GifsInfo] на текущей странице.
 * @property users Список авторов, встреченных в выборке.
 * @property niches Список связанных ниш.
 * @property tags Список тегов выборки.
 */
@Immutable
@Serializable
data class MediaResponse(
    @SerialName("page") val page: Int = 0,
    @SerialName("pages") val pages: Int = 0,
    @SerialName("total") val total: Int = 0,
    @SerialName("gifs") val gifs: List<GifsInfo> = emptyList(),
    @SerialName("users") val users: List<UserInfo> = emptyList(),
    @SerialName("niches") val niches: List<NichesInfo> = emptyList(),
    @SerialName("tags") val tags: List<String> = emptyList()
) {
    /** Проверяет, пуст ли ответ по всем типам контента (гифки, пользователи, ниши). */
    val isEmpty: Boolean get() = gifs.isEmpty() && users.isEmpty() && niches.isEmpty()

    /** Проверяет, содержит ли ответ хотя бы одну сущность. */
    val isNotEmpty: Boolean get() = !isEmpty

    /** Проверяет, является ли текущая страница первой. */
    val isFirstPage: Boolean get() = page <= 1

    /** Проверяет, есть ли следующие страницы для догрузки в пагинации. */
    val hasMorePages: Boolean get() = page < pages

    /** Проверяет наличие гифок в ответе. */
    val hasGifs: Boolean get() = gifs.isNotEmpty()

    /** Проверяет наличие пользователей в ответе. */
    val hasUsers: Boolean get() = users.isNotEmpty()

    /** Проверяет наличие ниш в ответе. */
    val hasNiches: Boolean get() = niches.isNotEmpty()

    /** Проверяет наличие тегов в ответе. */
    val hasTags: Boolean get() = tags.isNotEmpty()

    companion object {
        /** Пустой экземпляр ответа со значениями по умолчанию. */
        val EMPTY = MediaResponse()
    }
}
