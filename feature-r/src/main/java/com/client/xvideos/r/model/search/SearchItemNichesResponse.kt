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
) {
    val isEmpty: Boolean get() = niches.isEmpty()
    val isNotEmpty: Boolean get() = niches.isNotEmpty()
    val hasMorePages: Boolean get() = page < pages
    val size: Int get() = niches.size
    val isFirstPage: Boolean get() = page <= 1L

    companion object {
        val EMPTY = SearchNichesShortResponse()
    }
}

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
) {
    val displayName: String get() = name.ifBlank { id }
    val isValid: Boolean get() = id.isNotBlank()
    val hasThumbnail: Boolean get() = thumbnail.isNotBlank()
    val hasGifs: Boolean get() = gifs > 0L
    val hasSubscribers: Boolean get() = subscribers > 0L
    val hasTags: Boolean get() = tags.isNotEmpty()

    companion object {
        val EMPTY = SearchItemNichesResponse()
    }
}
