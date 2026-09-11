package com.client.xvideos.r.model.search

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchCreatorsResponse(
    @SerializedName("items") @SerialName("items") val items: List<SearchItemCreatorsResponse> = emptyList()
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
    @SerializedName("type") @SerialName("type") val type: String = "creator",
    @SerializedName("text") @SerialName("text") val text: String = "@elfsandi",
    @SerializedName("name") @SerialName("name") val name: String = "Ana",
    @SerializedName("image") @SerialName("image") val image: String? = null,
    @SerializedName("verified") @SerialName("verified") val verified: Boolean = true,
    @SerializedName("studio") @SerialName("studio") val studio: Boolean = false,
    @SerializedName("followers") @SerialName("followers") val followers: Long = 0L
)
