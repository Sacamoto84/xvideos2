package com.client.xvideos.l.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Все классы в файле поддерживают разбор через kotlinx.serialization.
// coerceInputValues безопасно подставляет дефолты при приходе null.

//--- landing_page_album ---

@Serializable
data class Landing_page_albumType(
    @SerialName("title") val title: String = "",
    @SerialName("sections") val sections: List<Landing_page_albumSection> = emptyList()
) {
    val isEmpty: Boolean get() = sections.isEmpty()
    val isNotEmpty: Boolean get() = sections.isNotEmpty()

    companion object {
        val EMPTY = Landing_page_albumType()
    }
}

@Serializable
data class Landing_page_albumSection(
    @SerialName("title") val title: String = "", //"Hentai Manga"
    @SerialName("count") val count: Int = 0, //10
    @SerialName("item_type") val itemType: String = "", //"album"
    @SerialName("url") val url: String = "",
    @SerialName("items") val items: List<Album> = emptyList()
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()

    companion object {
        val EMPTY = Landing_page_albumSection()
    }
}

//--- AlbumListTopHits ---
@Serializable
data class AlbumListTopHits(
    @SerialName("title")
    val title: String = "",

    @SerialName("url")
    val url: String = "",

    @SerialName("count")
    val count: Int = 0,

    @SerialName("item_type")
    val itemType: String = "",

    @SerialName("items")
    val items: List<Album> = emptyList()
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()

    companion object {
        val EMPTY = AlbumListTopHits()
    }
}

// Корневой класс для JSON
@Serializable
data class AlbumResponse(
    @SerialName("data")
    val data: AlbumData = AlbumData()
) {
    companion object {
        val EMPTY = AlbumResponse()
    }
}

@Serializable
data class AlbumData(
    @SerialName("album")
    val album: AlbumListWrapper = AlbumListWrapper()
) {
    companion object {
        val EMPTY = AlbumData()
    }
}

@Serializable
data class AlbumListWrapper(
    @SerialName("list")
    val list: AlbumList = AlbumList()
) {
    companion object {
        val EMPTY = AlbumListWrapper()
    }
}

@Serializable
data class AlbumList(
    @SerialName("info")
    val info: FacetCollectionInfo = FacetCollectionInfo(),
    @SerialName("items")
    val items: List<Album> = emptyList()
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()

    companion object {
        val EMPTY = AlbumList()
    }
}

@Serializable
data class FacetCollectionInfo(
    @SerialName("page")
    val page: Int = 0,
    @SerialName("has_next_page")
    val hasNextPage: Boolean = false,
    @SerialName("has_previous_page")
    val hasPreviousPage: Boolean = false,
    @SerialName("total_items")
    val totalItems: Int = 0,
    @SerialName("total_pages")
    val totalPages: Int = 0,
    @SerialName("items_per_page")
    val itemsPerPage: Int = 0,
    @SerialName("url_complete")
    val urlComplete: String = ""
) {
    val hasNext: Boolean get() = hasNextPage
    val hasPrevious: Boolean get() = hasPreviousPage
    val isFirstPage: Boolean get() = page <= 1

    companion object {
        val EMPTY = FacetCollectionInfo()
    }
}

@Serializable
data class Album(
    @SerialName("__typename") val typeName: String = "", // "Album"
    @SerialName("id") val id: String = "", // "551361"
    @SerialName("title") val title: String = "", // "Nimbletail art -- Comic strips"
    @SerialName("description") val description: String = "",
    @SerialName("like_status") val likeStatus: String = "", // "none"
    @SerialName("moderation_status") val moderationStatus: String = "", // "NOT_MODERATED"
    @SerialName("number_of_favorites") val numberOfFavorites: Int = 0, // 290
    @SerialName("number_of_dislikes") val numberOfDislikes: Int = 0, // 90
    @SerialName("number_of_pictures") val numberOfPictures: Int = 0, // 39
    @SerialName("number_of_animated_pictures") val numberOfAnimatedPictures: Int = 0, // 0
    @SerialName("number_of_duplicates") val numberOfDuplicates: Int = 0, // 0
    @SerialName("slug") val slug: String = "", // "nimbletail-art-comic-strips"
    @SerialName("is_manga") val isManga: Boolean = false, // true
    @SerialName("url") val url: String = "", // "/albums/nimbletail-art-comic-strips_551361/"
    @SerialName("download_url") val downloadUrl: String = "", // "/download/r/1392433/551361/"
    @SerialName("labels") val labels: List<String> = emptyList(), // [ "hot" ]
    @SerialName("permissions") val permissions: List<String> = emptyList(), // [ "can_add" ]
    @SerialName("cover") val cover: Cover? = null,
    @SerialName("language") val language: Language? = null,
    @SerialName("created_by") val createdBy: User = User(),
    @SerialName("tags") val tags: List<Tag> = emptyList(),
    @SerialName("genres") val genres: List<Genre> = emptyList()
) {
    val isValid: Boolean get() = id.isNotBlank()
    val hasCover: Boolean get() = cover?.isValid == true
    val hasPictures: Boolean get() = numberOfPictures > 0
    val hasAnimatedPictures: Boolean get() = numberOfAnimatedPictures > 0
    val hasTags: Boolean get() = tags.isNotEmpty()
    val hasGenres: Boolean get() = genres.isNotEmpty()

    companion object {
        val EMPTY = Album()
    }
}

@Serializable
data class Cover(
    @SerialName("width") val width: Int = 0,
    @SerialName("height") val height: Int = 0,
    @SerialName("size") val size: String = "",
    @SerialName("url") val url: String = ""
) {
    val isValid: Boolean get() = url.isNotBlank()

    companion object {
        val EMPTY = Cover()
    }
}

@Serializable
data class Language(
    @SerialName("id")
    val id: String = "",
    @SerialName("title")
    val title: String = "",
    @SerialName("url")
    val url: String = ""
) {
    val isValid: Boolean get() = id.isNotBlank() && title.isNotBlank()

    companion object {
        val EMPTY = Language()
    }
}

@Serializable
data class User(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("url")
    val url: String = ""
) {
    val effectiveName: String get() = displayName.ifBlank { name }
    val isValid: Boolean get() = id.isNotBlank()

    companion object {
        val EMPTY = User()
    }
}

@Serializable
data class Tag(
    @SerialName("id") val id: String = "",
    @SerialName("category") val category: String? = null,
    @SerialName("text") val text: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("count") val count: Int = 0
) {
    val isValid: Boolean get() = id.isNotBlank() && text.isNotBlank()

    companion object {
        val EMPTY = Tag()
    }
}

@Serializable
data class Genre(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("acts_as_warning") val actsAsWarning: Boolean = false,
    @SerialName("url") val url: String = ""
) {
    val isValid: Boolean get() = id.isNotBlank() && title.isNotBlank()

    companion object {
        val EMPTY = Genre()
    }
}

@Serializable
data class Audience(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("url") val url: String = ""
) {
    val isValid: Boolean get() = id.isNotBlank() && title.isNotBlank()

    companion object {
        val EMPTY = Audience()
    }
}
