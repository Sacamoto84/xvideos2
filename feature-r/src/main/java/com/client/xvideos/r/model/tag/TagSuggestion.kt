package com.client.xvideos.r.model.tag

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagSuggestion(
    @SerializedName("gifs") @SerialName("gifs") val gifs: Long = 0L,
    @SerializedName("text") @SerialName("text") val text: String = "",
    @SerializedName("type") @SerialName("type") val type: String = ""
)
