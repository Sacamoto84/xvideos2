package com.client.xvideos.r.model.search

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SearchCreatorsResponse(
    @SerialName("items") val items: List<SearchItemCreatorsResponse> = emptyList()
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()

    companion object {
        val EMPTY = SearchCreatorsResponse()
    }
}

//{
//    "type": "creator",
//    "text": "@elfsandi",
//    "name": "Ana \ud83d\udc8b",
//    "image": "https:\/\/userpic.redgifs.com\/5\/3f\/53f9367f4b1d523a032f5fa2475de70d.png",
//    "verified": true,
//    "studio": false,
//    "followers": 274
//},
//{
//    "type": "creator",
//    "text": "@ana-fernandez",
//    "name": "ana-fernandez",
//    "image": null,
//    "verified": false,
//    "studio": false,
//    "followers": 77
//},
@Immutable
@Serializable
data class SearchItemCreatorsResponse(
    @SerialName("type") val type: String = "creator",
    @SerialName("text") val text: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("image") val image: String? = null,
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("studio") val studio: Boolean = false,
    @SerialName("followers") val followers: Long = 0L
) {
    val username: String get() = text.removePrefix("@")
    val isValid: Boolean get() = text.isNotBlank()

    companion object {
        val EMPTY = SearchItemCreatorsResponse()
    }
}
