package com.client.xvideos.r.model.search

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SearchNichesShortResponse(
    @SerialName("page") val page: Long = 0L,
    @SerialName("pages") val pages: Long = 0L,
    @SerialName("total") val total: Long = 0L,
    @SerialName("niches") val niches: List<SearchItemNichesResponse> = emptyList()
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
@Immutable
@Serializable
data class SearchItemNichesResponse(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("gifs") val gifs: Long = 0L,
    @SerialName("subscribers") val subscribers: Long = 0L,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("preferences") val preferences: List<String> = emptyList(),
    @SerialName("thumbnail") val thumbnail: String = ""
)
