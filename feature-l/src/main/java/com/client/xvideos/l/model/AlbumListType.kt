package com.client.xvideos.l.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Все классы в файле поддерживают разбор как через kotlinx.serialization, так и через Gson.
// У non-null полей обязаны быть значения по умолчанию: Gson не знает про котлиновскую
// нуллабельность и кладёт null в отсутствующее поле, а kotlinx с coerceInputValues
// безопасно подставляет дефолты.

//--- landing_page_album ---

@Serializable
data class Landing_page_albumType(
    @SerializedName("title") @SerialName("title") val title: String = "",
    @SerializedName("sections") @SerialName("sections") val sections: List<Landing_page_albumSection> = emptyList()
)

@Serializable
data class Landing_page_albumSection(
    @SerializedName("title") @SerialName("title") val title: String = "", //"Hentai Manga"
    @SerializedName("count") @SerialName("count") val count: Int = 0, //10
    @SerializedName("item_type") @SerialName("item_type") val itemType: String = "", //"album"
    @SerializedName("url") @SerialName("url") val url: String = "",
    @SerializedName("items") @SerialName("items") val items: List<Album> = emptyList()
)

//--- AlbumListTopHits ---
@Serializable
data class AlbumListTopHits(
    @SerializedName("title")
    @SerialName("title")
    val title: String = "",

    @SerializedName("url")
    @SerialName("url")
    val url: String = "",

    @SerializedName("count")
    @SerialName("count")
    val count: Int = 0,

    @SerializedName("item_type")
    @SerialName("item_type")
    val itemType: String = "",

    @SerializedName("items")
    @SerialName("items")
    val items: List<Album> = emptyList()
)

// Корневой класс для JSON
@Serializable
data class AlbumResponse(
    @SerializedName("data")
    @SerialName("data")
    val data: AlbumData = AlbumData()
)

@Serializable
data class AlbumData(
    @SerializedName("album")
    @SerialName("album")
    val album: AlbumListWrapper = AlbumListWrapper()
)

@Serializable
data class AlbumListWrapper(
    @SerializedName("list")
    @SerialName("list")
    val list: AlbumList = AlbumList()
)

@Serializable
data class AlbumList(
    @SerializedName("info")
    @SerialName("info")
    val info: FacetCollectionInfo = FacetCollectionInfo(),
    @SerializedName("items")
    @SerialName("items")
    val items: List<Album> = emptyList()
)

@Serializable
data class FacetCollectionInfo(
    @SerializedName("page")
    @SerialName("page")
    val page: Int = 0,
    @SerializedName("has_next_page")
    @SerialName("has_next_page")
    val hasNextPage: Boolean = false,
    @SerializedName("has_previous_page")
    @SerialName("has_previous_page")
    val hasPreviousPage: Boolean = false,
    @SerializedName("total_items")
    @SerialName("total_items")
    val totalItems: Int = 0,
    @SerializedName("total_pages")
    @SerialName("total_pages")
    val totalPages: Int = 0,
    @SerializedName("items_per_page")
    @SerialName("items_per_page")
    val itemsPerPage: Int = 0,
    @SerializedName("url_complete")
    @SerialName("url_complete")
    val urlComplete: String = ""
)

@Serializable
data class Album(
    @SerializedName("__typename") @SerialName("__typename") val typeName: String = "", // "Album"
    @SerializedName("id") @SerialName("id") val id: String = "", // "551361"
    @SerializedName("title") @SerialName("title") val title: String = "", // "Nimbletail art -- Comic strips"
    @SerializedName("description") @SerialName("description") val description: String = "",
    @SerializedName("like_status") @SerialName("like_status") val likeStatus: String = "", // "none"
    @SerializedName("moderation_status") @SerialName("moderation_status") val moderationStatus: String = "", // "NOT_MODERATED"
    @SerializedName("number_of_favorites") @SerialName("number_of_favorites") val numberOfFavorites: Int = 0, // 290
    @SerializedName("number_of_dislikes") @SerialName("number_of_dislikes") val numberOfDislikes: Int = 0, // 90
    @SerializedName("number_of_pictures") @SerialName("number_of_pictures") val numberOfPictures: Int = 0, // 39
    @SerializedName("number_of_animated_pictures") @SerialName("number_of_animated_pictures") val numberOfAnimatedPictures: Int = 0, // 0
    @SerializedName("number_of_duplicates") @SerialName("number_of_duplicates") val numberOfDuplicates: Int = 0, // 0
    @SerializedName("slug") @SerialName("slug") val slug: String = "", // "nimbletail-art-comic-strips"
    @SerializedName("is_manga") @SerialName("is_manga") val isManga: Boolean = false, // true
    @SerializedName("url") @SerialName("url") val url: String = "", // "/albums/nimbletail-art-comic-strips_551361/"
    @SerializedName("download_url") @SerialName("download_url") val downloadUrl: String = "", // "/download/r/1392433/551361/"
    @SerializedName("labels") @SerialName("labels") val labels: List<String> = emptyList(), // [ "hot" ]
    @SerializedName("permissions") @SerialName("permissions") val permissions: List<String> = emptyList(), // [ "can_add" ]
    @SerializedName("cover") @SerialName("cover") val cover: Cover? = null,
    @SerializedName("language") @SerialName("language") val language: Language? = null,
    @SerializedName("created_by") @SerialName("created_by") val createdBy: User = User(),
    @SerializedName("tags") @SerialName("tags") val tags: List<Tag> = emptyList(),
    @SerializedName("genres") @SerialName("genres") val genres: List<Genre> = emptyList()
)

@Serializable
data class Cover(
    @SerializedName("width") @SerialName("width") val width: Int = 0,
    @SerializedName("height") @SerialName("height") val height: Int = 0,
    @SerializedName("size") @SerialName("size") val size: String = "",
    @SerializedName("url") @SerialName("url") val url: String = ""
)

@Serializable
data class Language(
    @SerializedName("id")
    @SerialName("id")
    val id: String = "",
    @SerializedName("title")
    @SerialName("title")
    val title: String = "",
    @SerializedName("url")
    @SerialName("url")
    val url: String = ""
)

@Serializable
data class User(
    @SerializedName("id")
    @SerialName("id")
    val id: String = "",
    @SerializedName("name")
    @SerialName("name")
    val name: String = "",
    @SerializedName("display_name")
    @SerialName("display_name")
    val displayName: String = "",
    @SerializedName("url")
    @SerialName("url")
    val url: String = ""
)

@Serializable
data class Tag(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("category") @SerialName("category") val category: String? = null,
    @SerializedName("text") @SerialName("text") val text: String = "",
    @SerializedName("url") @SerialName("url") val url: String = "",
    @SerializedName("count") @SerialName("count") val count: Int = 0
)

@Serializable
data class Genre(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("title") @SerialName("title") val title: String = "",
    @SerializedName("acts_as_warning") @SerialName("acts_as_warning") val actsAsWarning: Boolean = false,
    @SerializedName("url") @SerialName("url") val url: String = ""
)

@Serializable
data class Audience(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("title") @SerialName("title") val title: String = "",
    @SerializedName("url") @SerialName("url") val url: String = ""
)
