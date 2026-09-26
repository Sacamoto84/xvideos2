package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель профиля автора/создателя контента в RedGifs.
 *
 * Содержит счетчики подписчиков, опубликованных постов, суммарных просмотров,
 * ссылки на аватар и сторонние социальные сети.
 *
 * Пример JSON из API:
 * ```json
 * {
 *   "creationtime": 1758278234,
 *   "description": "Creator description",
 *   "followers": 21193,
 *   "following": 0,
 *   "gifs": 2324,
 *   "name": "relative_rub",
 *   "profileImageUrl": "https://userpic.redgifs.com/.../avatar.png",
 *   "profileUrl": "https://onlyfans.com/...",
 *   "publishedGifs": 2176,
 *   "url": "https://www.redgifs.com/users/relative_rub",
 *   "username": "relative_rub",
 *   "verified": true,
 *   "views": 32986108
 * }
 * ```
 *
 * @property description Текстовое био автора.
 * @property creationtime Время регистрации аккаунта (unix timestamp).
 * @property followers Количество подписчиков.
 * @property gifs Общее число загруженных гифок.
 * @property name Отображаемое имя (Display Name).
 * @property profileImageUrl URL аватара профиля.
 * @property profileUrl Внешняя ссылка на личный сайт/OnlyFans/linktree автора.
 * @property publishedGifs Количество публично опубликованных гифок.
 * @property url Прямая ссылка на страницу автора на redgifs.com.
 * @property username Уникальный никнейм автора в нижнем регистре.
 * @property verified Флаг подтвержденного (верифицированного) аккаунта.
 * @property views Общее число просмотров всех гифок автора.
 */
@Immutable
@Serializable
data class UserInfo(
    @SerialName("description")     val description: String? = null,       // * Описание в профиле
    @SerialName("creationtime")    val creationtime: Long = 0L,           // * Описание пользователя в его профиле.                     > "Collared sub addicted to XL horse dildos"
    @SerialName("followers")       val followers: Long = 0,               // * Количество подписчиков пользователя.                      > 68214
    @SerialName("gifs")            val gifs: Long = 0,                    // * Общее количество опубликованных пользователем GIF-файлов. > 439
    @SerialName("name")            val name: String = "",                 // * Имя пользователя. Большие буквы> "lilijunex"
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,   // * URL-адрес изображения профиля пользователя. > "https://userpic.redgifs.com/4/8c/48cc3668e114f878aafcc6dfd0a3d4f2.png"
    @SerialName("profileUrl")      val profileUrl: String = "",           // * URL-адрес профиля пользователя. Это URL, который отображается в профиле, установленном пользователем. Это НЕ URL пользователя на "redgifs.com" >"https://beacons.ai/lilijunex"
    @SerialName("publishedGifs")   val publishedGifs: Long = 0,             // * Количество опубликованных публичных GIF-файлов.       > 421 (Отображается в профиле)
    @SerialName("url")             val url: String = "",                              // * URL-адрес пользователя на сайте ``redgifs.com``. > "https://www.redgifs.com/users/lilijunex"
    @SerialName("username")        val username: String = "",                    // * Имя пользователя. маленькие буквы>"lilijunex"
    @SerialName("verified")        val verified: Boolean = false,                // *
    @SerialName("views")           val views: Long  = 0L,                           // * Общее количество просмотров всех опубликованных пользователем GIF. > 123194825
) {
    /** Проверяет валидность модели (username не пуст). */
    val isValid: Boolean get() = username.isNotBlank()

    /** Возвращает отображаемое имя автора (если name пустое, берется username). */
    val displayName: String get() = name.ifBlank { username }

    /** Проверяет наличие аватара. */
    val hasAvatar: Boolean get() = !profileImageUrl.isNullOrBlank()

    /** Проверяет наличие описания. */
    val hasDescription: Boolean get() = !description.isNullOrBlank()

    /** Проверяет наличие времени регистрации. */
    val hasCreationTime: Boolean get() = creationtime > 0L

    /** Проверяет наличие подписчиков. */
    val hasFollowers: Boolean get() = followers > 0L

    /** Проверяет наличие опубликованных гифок. */
    val hasGifs: Boolean get() = gifs > 0L || publishedGifs > 0L

    /** Проверяет наличие внешней ссылки на профиль. */
    val hasProfileUrl: Boolean get() = profileUrl.isNotBlank()

    /** Проверяет статус верификации. */
    val isVerified: Boolean get() = verified

    /** Проверяет наличие просмотров. */
    val hasViews: Boolean get() = views > 0L

    /** Нормализованное имя пользователя (в нижнем регистре без лишних пробелов). */
    val normalizedUsername: String get() = username.trim().lowercase()

    /** Проверяет совпадение пользователей по юзернейму. */
    fun isSameUser(other: UserInfo?): Boolean =
        other != null && isValid && normalizedUsername == other.normalizedUsername

    /** Проверяет соответствие автора поисковому запросу по отображаемому имени или никнейму. */
    fun matches(query: String?): Boolean =
        if (query.isNullOrBlank()) false else displayName.contains(query.trim(), ignoreCase = true) || username.contains(query.trim(), ignoreCase = true)

    companion object {
        /** Пустой экземпляр [UserInfo]. */
        val EMPTY = UserInfo()
    }
}
