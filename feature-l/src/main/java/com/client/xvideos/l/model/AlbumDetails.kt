package com.client.xvideos.l.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Поддерживает разбор как через kotlinx.serialization (сетевой слой), так и
 * через Gson (дисковый кэш LAlbumBundleCache).
 *
 * У non-null полей обязаны быть значения по умолчанию: Gson не проверяет
 * котлиновскую нуллабельность, а coerceInputValues в kotlinx.serialization
 * безопасно подставляет дефолты при приходе null.
 */
@Serializable
@Suppress("ConstructorParameterNaming")
data class AlbumDetails(
    @SerializedName("created") @SerialName("created") val created: Double = 0.0, // Время создания альбома 1780919842.393262
    @SerializedName("modified") @SerialName("modified") val modified: Double = 0.0, // Время последнего изменения альбома 1780920373.633646

    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("title") @SerialName("title") val title: String = "",
    @SerializedName("tags") @SerialName("tags") val tags: List<Tag> = emptyList(),
    @SerializedName("is_manga") @SerialName("is_manga") val is_manga: Boolean = false,
    @SerializedName("content") @SerialName("content") val content: Content = Content(),
    @SerializedName("genres") @SerialName("genres") val genres: List<Genre> = emptyList(),
    @SerializedName("cover") @SerialName("cover") val cover: Cover? = null,
    @SerializedName("description") @SerialName("description") val description: String = "", //Возвращает описание альбома
    @SerializedName("audiences") @SerialName("audiences") val audiences: List<Audience> = emptyList(),
    @SerializedName("number_of_pictures") @SerialName("number_of_pictures") val number_of_pictures: Int = 0, //Возвращает количество фотографий в альбоме (в это число входят и gif-файлы).
    @SerializedName("number_of_animated_pictures") @SerialName("number_of_animated_pictures") val number_of_animated_pictures: Int = 0,
    @SerializedName("url") @SerialName("url") val url: String = "",
    @SerializedName("download_url") @SerialName("download_url") val download_url: String = "",
    @SerializedName("slug") @SerialName("slug") val slug: String? = null,
    @SerializedName("like_status") @SerialName("like_status") val likeStatus: String? = null,
    @SerializedName("moderation_status") @SerialName("moderation_status") val moderationStatus: String? = null,
    @SerializedName("number_of_favorites") @SerialName("number_of_favorites") val numberOfFavorites: Int? = null,
    @SerializedName("number_of_dislikes") @SerialName("number_of_dislikes") val numberOfDislikes: Int? = null,
    @SerializedName("number_of_duplicates") @SerialName("number_of_duplicates") val numberOfDuplicates: Int? = null,
    @SerializedName("labels") @SerialName("labels") val labels: List<String> = emptyList(),
    @SerializedName("permissions") @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerializedName("language") @SerialName("language") val language: Language? = null,
    @SerializedName("created_by") @SerialName("created_by") val createdBy: User? = null
)

@Serializable
data class Content(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("title") @SerialName("title") val title: String = "",
    @SerializedName("url") @SerialName("url") val url: String = ""
)
