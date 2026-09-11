package com.client.xvideos.r.model.search

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchNichesShortResponse(
    @SerializedName("page") @SerialName("page") val page: Long = 0L,
    @SerializedName("pages") @SerialName("pages") val pages: Long = 0L,
    @SerializedName("total") @SerialName("total") val total: Long = 0L,
    @SerializedName("niches") @SerialName("niches") val niches: List<SearchItemNichesResponse> = emptyList()
)

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
@Serializable
data class SearchItemNichesResponse(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("name") @SerialName("name") val name: String = "",
    @SerializedName("gifs") @SerialName("gifs") val gifs: Long = 0L,
    @SerializedName("subscribers") @SerialName("subscribers") val subscribers: Long = 0L,
    @SerializedName("tags") @SerialName("tags") val tags: List<String> = emptyList(),
    @SerializedName("preferences") @SerialName("preferences") val preferences: List<String> = emptyList(),
    @SerializedName("thumbnail") @SerialName("thumbnail") val thumbnail: String = ""
)
