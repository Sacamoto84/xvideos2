package com.client.xvideos.l.model

import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank

data class AlbumListFilter(
    val display: String = "date_newest",
    val album_type: AlbumType = AlbumType.Pictures, //manga pictures или все при отсуствии
    val audienceIds: String = "+1+10+12+2+3+5+6+8+9",
    val languageIds: String = "+1+100+101+2+3+4+5+6+7+8+9+99", //Все языки
    val itemsPerPage: Int = 30,
    val picture_count_rank : PictureCountRank = PictureCountRank.All,
    val content_id : ContentId = ContentId.All,
    val genresPlus : List<FilterGenre> = emptyList(),
    val genresMinus: List<FilterGenre> = emptyList(),
    val tagPlus : List<String> = emptyList(),
    val tagMinus : List<String> = emptyList(),
    val searchQuery : String = "",
    val selection : String = "",
    )
