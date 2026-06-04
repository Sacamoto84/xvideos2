package com.client.xvideos.r.model.search

import com.google.gson.annotations.SerializedName

data class SearchCreatorsResponse(
    @SerializedName("items") val items: List<SearchItemCreatorsResponse>
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
data class SearchItemCreatorsResponse(
    @SerializedName("type")  val type: String = "creator",
    @SerializedName("text")  val  text: String = "@elfsandi",
    @SerializedName("name")  val name: String ="Ana",
    @SerializedName("image")  val image: String?,
    @SerializedName("verified") val verified: Boolean = true,
    @SerializedName("studio") val studio: Boolean =  false,
    @SerializedName("followers") val followers : Long = 0

)