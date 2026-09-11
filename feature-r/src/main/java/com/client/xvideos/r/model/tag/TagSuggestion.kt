package com.client.xvideos.r.model.tag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagSuggestion(
    @SerialName("gifs") val gifs: Long = 0L,
    @SerialName("text") val text: String = "",
    @SerialName("type") val type: String = ""
)
