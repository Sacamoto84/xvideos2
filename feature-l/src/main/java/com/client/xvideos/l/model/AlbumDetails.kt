package com.client.xvideos.l.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Поддерживает разбор через kotlinx.serialization.
 *
 * coerceInputValues в kotlinx.serialization безопасно подставляет дефолты
 * при приходе null.
 */
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
)

@Serializable
data class Content(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("url") val url: String = ""
)
