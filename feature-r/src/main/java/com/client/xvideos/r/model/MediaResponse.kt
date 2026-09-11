package com.client.xvideos.r.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaResponse(
    @SerializedName("page") @SerialName("page") val page: Int = 0,
    @SerializedName("pages") @SerialName("pages") val pages: Int = 0,
    @SerializedName("total") @SerialName("total") val total: Int = 0,
    @SerializedName("gifs") @SerialName("gifs") val gifs: List<GifsInfo> = emptyList(),
    @SerializedName("users") @SerialName("users") val users: List<UserInfo> = emptyList(),
    @SerializedName("niches") @SerialName("niches") val niches: List<NichesInfo> = emptyList(),
    @SerializedName("tags") @SerialName("tags") val tags: List<String> = emptyList()
)
