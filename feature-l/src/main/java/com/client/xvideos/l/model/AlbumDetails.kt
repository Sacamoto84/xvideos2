package com.client.xvideos.l.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Подробная модель альбома Luscious, получаемая по запросу `getAlbumInfo`.
 *
 * Поддерживает разбор через kotlinx.serialization.
 * coerceInputValues в kotlinx.serialization безопасно подставляет дефолты
 * при приходе null.
 *
 * @property created Время создания альбома (Unix timestamp double).
 * @property modified Время последнего изменения альбома.
 * @property id Уникальный числовой ID альбома в виде строки.
 * @property title Название альбома.
 * @property tags Список тегов альбома.
 * @property is_manga Признак того, что альбом является мангой/комиксом.
 * @property content Категория контента альбома.
 * @property genres Список жанров альбома.
 * @property cover Обложка альбома [Cover].
 * @property description Текстовое описание альбома.
 * @property audiences Список целевых аудиторий альбома.
 * @property number_of_pictures Общее количество картинок в альбоме.
 * @property number_of_animated_pictures Число анимированных изображений (GIF/видео).
 * @property url Относительный URL страницы альбома.
 * @property download_url Относительный URL скачивания ZIP-архива альбома.
 * @property slug Человекочитаемый URL-слаг альбома.
 * @property likeStatus Текущий статус лайка пользователя (`like`, `none`).
 * @property moderationStatus Статус модерации альбома.
 * @property numberOfFavorites Число добавлений в избранное на сервере.
 * @property numberOfDislikes Число дислайков.
 * @property numberOfDuplicates Количество дубликатов.
 * @property labels Метки альбома (например, `"hot"`).
 * @property permissions Разрешения пользователя для альбома.
 * @property language Язык контента альбома.
 * @property createdBy Автор, создавший альбом.
 */
@Immutable
@Serializable
@Suppress("ConstructorParameterNaming")
data class AlbumDetails(
    @SerialName("created") val created: Double = 0.0, // Время создания альбома 1780919842.393262
    @SerialName("modified") val modified: Double = 0.0, // Время последнего изменения альбома 1780920373.633646

    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("tags") val tags: List<Tag> = emptyList(),
    @SerialName("is_manga") val is_manga: Boolean = false,
    @SerialName("content") val content: Content = Content(),
    @SerialName("genres") val genres: List<Genre> = emptyList(),
    @SerialName("cover") val cover: Cover? = null,
    @SerialName("description") val description: String = "", //Возвращает описание альбома
    @SerialName("audiences") val audiences: List<Audience> = emptyList(),
    @SerialName("number_of_pictures") val number_of_pictures: Int = 0, //Возвращает количество фотографий в альбоме (в это число входят и gif-файлы).
    @SerialName("number_of_animated_pictures") val number_of_animated_pictures: Int = 0,
    @SerialName("url") val url: String = "",
    @SerialName("download_url") val download_url: String = "",
    @SerialName("slug") val slug: String? = null,
    @SerialName("like_status") val likeStatus: String? = null,
    @SerialName("moderation_status") val moderationStatus: String? = null,
    @SerialName("number_of_favorites") val numberOfFavorites: Int? = null,
    @SerialName("number_of_dislikes") val numberOfDislikes: Int? = null,
    @SerialName("number_of_duplicates") val numberOfDuplicates: Int? = null,
    @SerialName("labels") val labels: List<String> = emptyList(),
    @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerialName("language") val language: Language? = null,
    @SerialName("created_by") val createdBy: User? = null
) {
    val isValid: Boolean get() = hasValidId()
    val hasDownloadUrl: Boolean get() = download_url.isNotBlank()
    val hasPictures: Boolean get() = number_of_pictures > 0
    val hasAnimatedPictures: Boolean get() = number_of_animated_pictures > 0
    val hasCover: Boolean get() = cover?.isValid == true
    val hasTags: Boolean get() = tags.isNotEmpty()
    val hasGenres: Boolean get() = genres.isNotEmpty()
    val hasAudiences: Boolean get() = audiences.isNotEmpty()
    val hasDescription: Boolean get() = description.isNotBlank()
    val hasLanguage: Boolean get() = language?.isValid == true
    val hasCreatedBy: Boolean get() = createdBy?.isValid == true
    val effectiveTitle: String get() = title.ifBlank { id }
    val numericId: Long get() = id.toLongOrNull() ?: 0L
    val isLiked: Boolean get() = likeStatus.equals("like", ignoreCase = true)
    val tagsCount: Int get() = tags.size
    val genresCount: Int get() = genres.size
    val audiencesCount: Int get() = audiences.size

    fun containsTag(tagName: String?): Boolean =
        if (tagName.isNullOrBlank()) false else tags.any { it.text.equals(tagName, ignoreCase = true) }

    fun containsGenre(genreTitle: String?): Boolean =
        if (genreTitle.isNullOrBlank()) false else genres.any { it.title.equals(genreTitle, ignoreCase = true) }


    companion object {
        val EMPTY = AlbumDetails()
    }
}

/**
 * Категория контента альбома.
 */
@Immutable
@Serializable
data class Content(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("url") val url: String = ""
) {
    val isValid: Boolean get() = id.isNotBlank()
    val hasTitle: Boolean get() = title.isNotBlank()

    companion object {
        val EMPTY = Content()
    }
}

/**
 * Возвращает URL обложки альбома при наличии.
 */
fun AlbumDetails.coverUrl(): String? = cover?.url?.takeIf { it.isNotBlank() }

/**
 * Проверяет, что у альбома валидный числовой ID.
 */
fun AlbumDetails.hasValidId(): Boolean = id.isNotBlank() && id.toLongOrNull() != null
