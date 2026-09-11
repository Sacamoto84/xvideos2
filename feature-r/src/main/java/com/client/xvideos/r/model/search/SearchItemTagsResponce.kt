package com.client.xvideos.r.model.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//"type": "tag",
//"text": "Anal",
//"gifs": 752986
@Serializable
data class SearchItemTagsResponse(
    @SerialName("type") val type: String = "tag",
    @SerialName("text") val text: String = "",
    @SerialName("gifs") val gifs: Long = 0L
)
