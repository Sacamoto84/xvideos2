package com.client.xvideos.r.model.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchCreatorsResponse(
    @SerialName("items") val items: List<SearchItemCreatorsResponse> = emptyList()
)

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
@Serializable
data class SearchItemCreatorsResponse(
    @SerialName("type") val type: String = "creator",
    @SerialName("text") val text: String = "@elfsandi",
    @SerialName("name") val name: String = "Ana",
    @SerialName("image") val image: String? = null,
    @SerialName("verified") val verified: Boolean = true,
    @SerialName("studio") val studio: Boolean = false,
    @SerialName("followers") val followers: Long = 0L
)
