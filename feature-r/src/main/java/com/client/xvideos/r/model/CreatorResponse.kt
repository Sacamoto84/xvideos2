package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ сетевого запроса контента конкретного автора (`/v2/users/{username}/search`).
 *
 * Содержит список роликов автора, данные его профиля, ниши и параметры пагинации.
 *
 * @property gifs Список гифок автора на запрошенной странице.
 * @property users Список с информацией о пользователе.
 * @property niches Список связанных ниш.
 * @property tags Список тегов.
 * @property page Номер текущей страницы.
 * @property pages Общее число страниц.
 * @property total Общее количество гифок у автора.
 */
@Immutable
@Serializable
data class CreatorResponse(
    @SerialName("gifs") val gifs: List<GifsInfo> = emptyList(),
    @SerialName("users") val users: List<UserInfo> = emptyList(),
    @SerialName("niches") val niches: List<NichesInfo> = emptyList(),
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("pages") val pages: Int = 0,
    @SerialName("total") val total: Int = 0,
) {
    /** Проверяет, пуст ли ответ по всем категориям. */
    val isEmpty: Boolean get() = gifs.isEmpty() && users.isEmpty() && niches.isEmpty()

    /** Проверяет, не пуст ли ответ. */
    val isNotEmpty: Boolean get() = !isEmpty

    /** Проверяет, является ли страница первой. */
    val isFirstPage: Boolean get() = page <= 1

    /** Проверяет наличие последующих страниц для пагинации. */
    val hasMorePages: Boolean get() = page < pages

    /** Проверяет, является ли страница последней. */
    val isLastPage: Boolean get() = pages > 0 && page >= pages

    /** Возвращает основной объект автора из списка users. */
    val primaryUser: UserInfo? get() = users.firstOrNull()

    /** Проверяет наличие гифок в ответе. */
    val hasGifs: Boolean get() = gifs.isNotEmpty()

    /** Количество гифок в ответе. */
    val gifsCount: Int get() = gifs.size

    /** Проверяет наличие данных автора. */
    val hasUsers: Boolean get() = users.isNotEmpty()

    /** Проверяет наличие ниш в ответе. */
    val hasNiches: Boolean get() = niches.isNotEmpty()

    /** Проверяет наличие тегов. */
    val hasTags: Boolean get() = tags.isNotEmpty()

    /** Находит гифку по ее ID или null. */
    fun findGifByIdOrNull(id: String?): GifsInfo? =
        if (id.isNullOrBlank()) null else gifs.firstOrNull { it.id == id }

    /** Находит нишу по ее имени или null. */
    fun findNicheByNameOrNull(name: String?): NichesInfo? =
        if (name.isNullOrBlank()) null else niches.firstOrNull { it.name.equals(name, ignoreCase = true) }

    companion object {
        /** Пустой экземпляр [CreatorResponse]. */
        val EMPTY = CreatorResponse()
    }
}
