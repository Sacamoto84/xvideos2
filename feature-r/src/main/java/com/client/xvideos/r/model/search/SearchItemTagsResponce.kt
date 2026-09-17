package com.client.xvideos.r.model.search

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//"type": "tag",
//"text": "Anal",
//"gifs": 752986
@Immutable
@Serializable
data class SearchItemTagsResponse(
    @SerialName("type") val type: String = "tag",
    @SerialName("text") val text: String = "",
    @SerialName("gifs") val gifs: Long = 0L
)
