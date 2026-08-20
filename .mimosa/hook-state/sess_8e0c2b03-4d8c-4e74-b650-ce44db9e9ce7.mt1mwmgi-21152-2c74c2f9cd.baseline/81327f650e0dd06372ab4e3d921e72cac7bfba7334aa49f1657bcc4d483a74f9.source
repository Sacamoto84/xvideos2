package com.client.xvideos.r.model.search

import com.google.gson.annotations.SerializedName


data class SearchNichesShortResponse(
    @SerializedName("page") val page : Long,
    @SerializedName("pages") val pages : Long,
    @SerializedName("total") val total : Long,
    @SerializedName("niches") val niches : List<SearchItemNichesResponse>
)


//"type": "niche",
//"text": "Anal Sex",
//"id": "anal-sex",
//"image": "https://userpic.redgifs.com/niches/thumbnails/anal-sex-f764b259.jpg",
//"subscribers": "276347"

/**
{
    "id": "real-orgasms",
    "name": "Real Orgasms",
    "gifs": 206007,
    "subscribers": 457411,
    "tags": [
    "Orgasm",
    "Orgasms",
    "Post Orgasm",
    "Real Orgasm"
    ],
    "preferences": [
    "bisexual",
    "lesbian",
    "straight"
    ],
    "thumbnail": "https://userpic.redgifs.com/niches/thumbnails/orgasms.jpg"
}
*/
data class SearchItemNichesResponse(
    @SerializedName("id")  val id: String,
    @SerializedName("name")  val name: String ,
    @SerializedName("gifs")  val  gifs: Long ,
    @SerializedName("subscribers") val subscribers : Long,
    @SerializedName("tags")  val  tags: List<String> ,
    @SerializedName("preferences")  val  preferences: List<String> = emptyList(),
    @SerializedName("thumbnail")  val thumbnail : String
)
