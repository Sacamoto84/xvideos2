package com.client.xvideos.l.model

import com.google.gson.annotations.SerializedName

// Все классы в файле разбираются Gson через рефлексию. У non-null полей обязаны
// быть значения по умолчанию: Gson не знает про котлиновскую нуллабельность и
// кладёт null в отсутствующее поле, а падает потом уже в UI.

//--- landing_page_album ---

data class Landing_page_albumType(
    @SerializedName("title") val title: String = "",
    @SerializedName("sections") val sections: List<Landing_page_albumSection> = emptyList()
)

data class Landing_page_albumSection(
    @SerializedName("title") val title: String = "",         //"Hentai Manga"
    @SerializedName("count") val count: Int = 0,            //10
    @SerializedName("item_type") val itemType: String = "",      //"album"
    @SerializedName("url") val url: String = "",           //"/albums/list/?album_type=manga&audience_ids=%2B1%2B10%2B12%2B2%2B3%2B5%2B6%2B8%2B9&display=date_trending&language_ids=%2B1%2B100%2B101%2B2%2B3%2B4%2B5%2B6%2B7%2B8%2B9%2B99&tagged=%2Bcollared&page=1"
    @SerializedName("items") val items: List<Album> = emptyList()
)







//--- AlbumListTopHits ---
data class AlbumListTopHits(
    @SerializedName("title")
    val title: String = "",

    @SerializedName("url")
    val url: String = "",

    @SerializedName("count")
    val count: Int = 0,

    @SerializedName("item_type")
    val itemType: String = "",

    @SerializedName("items")
    val items: List<Album> = emptyList()
)

// Корневой класс для JSON
data class AlbumResponse(
    @SerializedName("data")
    val data: AlbumData = AlbumData()
)

data class AlbumData(
    @SerializedName("album")
    val album: AlbumListWrapper = AlbumListWrapper()
)

data class AlbumListWrapper(
    @SerializedName("list")
    val list: AlbumList = AlbumList()
)

data class AlbumList(
    @SerializedName("info")
    val info: FacetCollectionInfo = FacetCollectionInfo(),
    @SerializedName("items")
    val items: List<Album> = emptyList()
)

data class FacetCollectionInfo(
    @SerializedName("page")
    val page: Int = 0,
    @SerializedName("has_next_page")
    val hasNextPage: Boolean = false,
    @SerializedName("has_previous_page")
    val hasPreviousPage: Boolean = false,
    @SerializedName("total_items")
    val totalItems: Int = 0,
    @SerializedName("total_pages")
    val totalPages: Int = 0,
    @SerializedName("items_per_page")
    val itemsPerPage: Int = 0,
    @SerializedName("url_complete")
    val urlComplete: String = ""
)

data class Album(
    @SerializedName("__typename")          val typeName: String = "",           // "Album"
    @SerializedName("id")                  val id: String = "",                 // "551361"
    @SerializedName("title")               val title: String = "",              // "Nimbletail art -- Comic strips"
    @SerializedName("description")         val description: String = "",        // "An album of Nimbletail's short comic strips.\n\nCheck out this album for hentai images https://members.luscious.net/albums/nimble-tail-art-images_512240/\n\nPS: This is not all that the artist has to offer, but a selection of what I like."
    //@SerializedName("created")             val created: Double,              // 1730508154
    //@SerializedName("modified")            val modified: Long,             // 1756516075
    @SerializedName("like_status")         val likeStatus: String = "",         // "none"
    @SerializedName("moderation_status")   val moderationStatus: String = "",   // "NOT_MODERATED"
    @SerializedName("number_of_favorites") val numberOfFavorites: Int = 0,     // 290
    @SerializedName("number_of_dislikes")  val numberOfDislikes: Int = 0,      // 90
    @SerializedName("number_of_pictures")  val numberOfPictures: Int = 0,      // 39
    @SerializedName("number_of_animated_pictures")    val numberOfAnimatedPictures: Int = 0, // 0
    @SerializedName("number_of_duplicates")           val numberOfDuplicates: Int = 0,       // 0
    @SerializedName("slug")         val slug: String = "",              // "nimbletail-art-comic-strips"
    @SerializedName("is_manga")     val isManga: Boolean = false,          // true
    @SerializedName("url")          val url: String = "",               // "/albums/nimbletail-art-comic-strips_551361/"
    @SerializedName("download_url") val downloadUrl: String = "",       // "/download/r/1392433/551361/"
    @SerializedName("labels")       val labels: List<String> = emptyList(),      // [ "hot" ]
    @SerializedName("permissions")  val permissions: List<String> = emptyList(), // [ "can_add" ]
    @SerializedName("cover")        val cover: Cover? = null,
    @SerializedName("language")     val language: Language? = null,  // {"id":"1","title":"English","url":"/languages/english_1/"}
    @SerializedName("created_by")   val createdBy: User = User(),    // { "id": "1392433", name": "Feloy_The_Furry", "display_name": "Feloy_The_Furry", "url": "/users/1392433/" }
    @SerializedName("tags")         val tags: List<Tag> = emptyList(),    // [ {"id": "1865756", "category": null, "text": "bitch suit", "url": "/tags/bitch_suit/?type=album",  "count": 6}, ...
    @SerializedName("genres")       val genres: List<Genre> = emptyList() // [ {"id": "27", "title": "BDSM", "acts_as_warning": false, "url": "/genres/bdsm_27/?type=album"} ],...
)

data class Cover(
    @SerializedName("width")  val width: Int = 0,
    @SerializedName("height") val height: Int = 0,
    @SerializedName("size")   val size: String = "",
    @SerializedName("url")    val url: String = ""
)

data class Language(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("title")
    val title: String = "",
    @SerializedName("url")
    val url: String = ""
)

data class User(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("display_name")
    val displayName: String = "",
    @SerializedName("url")
    val url: String = ""
)

data class Tag(
    @SerializedName("id")       val id: String = "",
    @SerializedName("category") val category: String? = null,
    @SerializedName("text")     val text: String = "",
    @SerializedName("url")      val url: String = "",
    @SerializedName("count")    val count: Int = 0
)

data class Genre(
    @SerializedName("id")              val id: String = "",
    @SerializedName("title")           val title: String = "",
    @SerializedName("acts_as_warning") val actsAsWarning: Boolean = false,
    @SerializedName("url")             val url: String = ""
)

data class Audience(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("url") val url: String = ""
)
