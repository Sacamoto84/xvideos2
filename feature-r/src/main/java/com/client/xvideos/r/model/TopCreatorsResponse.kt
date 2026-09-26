package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ эндпоинта топовых создателей в нише (`/v2/niches/{niche}/top-creators`).
 *
 * @property creators Список лучших авторов [TopCreator].
 */
@Immutable
@Serializable
data class TopCreatorsResponse(
    @SerialName("creators") val creators: List<TopCreator> = emptyList()
) {
    /** Проверяет, пуст ли список создателей. */
    val isEmpty: Boolean get() = creators.isEmpty()

    /** Проверяет, есть ли хотя бы один создатель. */
    val isNotEmpty: Boolean get() = creators.isNotEmpty()

    /** Количество создателей в ответе. */
    val size: Int get() = creators.size

    companion object {
        /** Пустой экземпляр ответа. */
        val EMPTY = TopCreatorsResponse()
    }
}

/**
 * Модель топового автора ниши с краткой статистикой.
 *
 * @property creationtime Время регистрации аккаунта.
 * @property description Описание профиля.
 * @property followers Число подписчиков.
 * @property gifs Общее количество гифок.
 * @property name Отображаемое имя.
 * @property profileImageUrl URL аватара.
 * @property username Уникальный никнейм автора.
 * @property verified Статус верификации.
 * @property studio Флаг профессиональной студии.
 * @property views Количество просмотров.
 */
@Immutable
@Serializable
data class TopCreator(
    @SerialName("creationtime") val creationtime: Long = 0L,
    @SerialName("description") val description: String = "",
    @SerialName("followers") val followers: Int = 0,
    @SerialName("gifs") val gifs: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("profileImageUrl") val profileImageUrl: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("studio") val studio: Boolean = false,
    @SerialName("views") val views: Int = 0
) {
    /** Проверяет валидность модели (username не пуст). */
    val isValid: Boolean get() = username.isNotBlank()

    /** Проверяет пустоту username. */
    val isEmpty: Boolean get() = username.isEmpty()

    /** Проверяет непустоту username. */
    val isNotEmpty: Boolean get() = username.isNotEmpty()

    /** Отображаемое имя автора или username как fallback. */
    val displayName: String get() = name.ifBlank { username }

    /** Проверяет наличие аватара. */
    val hasAvatar: Boolean get() = profileImageUrl.isNotBlank()

    /** Проверяет наличие гифок. */
    val hasGifs: Boolean get() = gifs > 0

    /** Проверяет наличие подписчиков. */
    val hasFollowers: Boolean get() = followers > 0

    companion object {
        /** Пустой экземпляр [TopCreator]. */
        val EMPTY = TopCreator()
    }
}
