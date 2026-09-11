package com.client.xvideos.r.model.search

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//"type": "tag",
//"text": "Anal",
//"gifs": 752986
@Serializable
data class SearchItemTagsResponse(
    @SerializedName("type") @SerialName("type") val type: String = "tag",
    @SerializedName("text") @SerialName("text") val text: String = "",
    @SerializedName("gifs") @SerialName("gifs") val gifs: Long = 0L
)
