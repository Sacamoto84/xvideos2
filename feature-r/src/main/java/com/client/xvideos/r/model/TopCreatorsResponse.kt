package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TopCreatorsResponse(
    @SerialName("creators") val creators: List<TopCreator> = emptyList()
)

@Immutable
@Serializable
data class TopCreator(
    @SerialName("creationtime") val creationtime: Long = 0L,
    @SerialName("description") val description: String = "",
    @SerialName("followers") val followers: Int = 0,
    @SerialName("gifs") val gifs: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("profileImageUrl") val profileImageUrl: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("studio") val studio: Boolean = false,
    @SerialName("views") val views: Int = 0
)
