package com.client.xvideos.l.model

import com.google.gson.annotations.SerializedName

/**
 * Разбирается Gson через рефлексию, поэтому у non-null полей обязаны быть
 * значения по умолчанию: Gson не проверяет котлиновскую нуллабельность и
 * молча кладёт null в отсутствующее поле, а NPE потом вылетает в UI.
 */
data class AlbumDetails(
    @SerializedName("created") val created: Double = 0.0, // Время создания альбома 1780919842.393262
    @SerializedName("modified") val modified: Double = 0.0, // Время последнего изменения альбома 1780920373.633646

    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("tags") val tags: List<Tag> = emptyList(),
    @SerializedName("is_manga") val is_manga: Boolean = false,
    @SerializedName("content") val content: Content = Content(),
    @SerializedName("genres") val genres: List<Genre> = emptyList(),
    @SerializedName("cover") val cover: Cover? = null,
    @SerializedName("description") val description: String = "",            //Возвращает описание альбома
    @SerializedName("audiences") val audiences: List<Audience> = emptyList(),
    @SerializedName("number_of_pictures") val number_of_pictures: Int = 0, //Возвращает количество фотографий в альбоме (в это число входят и gif-файлы).
    @SerializedName("number_of_animated_pictures") val number_of_animated_pictures: Int = 0, //
    @SerializedName("url") val url: String = "",
    @SerializedName("download_url") val download_url: String = "",
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("like_status") val likeStatus: String? = null,
    @SerializedName("moderation_status") val moderationStatus: String? = null,
    @SerializedName("number_of_favorites") val numberOfFavorites: Int? = null,
    @SerializedName("number_of_dislikes") val numberOfDislikes: Int? = null,
    @SerializedName("number_of_duplicates") val numberOfDuplicates: Int? = null,
    @SerializedName("labels") val labels: List<String> = emptyList(),
    @SerializedName("permissions") val permissions: List<String> = emptyList(),
    @SerializedName("language") val language: Language? = null,
    @SerializedName("created_by") val createdBy: User? = null
)


data class Content(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("url") val url: String = ""
)
