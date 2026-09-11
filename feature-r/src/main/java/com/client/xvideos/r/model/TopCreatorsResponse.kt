package com.client.xvideos.r.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopCreatorsResponse(
    @SerializedName("creators") @SerialName("creators") val creators: List<TopCreator> = emptyList()
)

@Serializable
data class TopCreator(
    @SerializedName("creationtime") @SerialName("creationtime") val creationtime: Long = 0L,
    @SerializedName("description") @SerialName("description") val description: String = "",
    @SerializedName("followers") @SerialName("followers") val followers: Int = 0,
    @SerializedName("gifs") @SerialName("gifs") val gifs: Int = 0,
    @SerializedName("name") @SerialName("name") val name: String = "",
    @SerializedName("profileImageUrl") @SerialName("profileImageUrl") val profileImageUrl: String = "",
    @SerializedName("username") @SerialName("username") val username: String = "",
    @SerializedName("verified") @SerialName("verified") val verified: Boolean = false,
    @SerializedName("studio") @SerialName("studio") val studio: Boolean = false,
    @SerializedName("views") @SerialName("views") val views: Int = 0
)
