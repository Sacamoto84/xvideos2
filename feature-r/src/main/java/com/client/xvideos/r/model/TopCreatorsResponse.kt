package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TopCreatorsResponse(
    @SerialName("creators") val creators: List<TopCreator> = emptyList()
) {
    val isEmpty: Boolean get() = creators.isEmpty()
    val isNotEmpty: Boolean get() = creators.isNotEmpty()
    val size: Int get() = creators.size

    companion object {
        val EMPTY = TopCreatorsResponse()
    }
}

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
) {
    val isValid: Boolean get() = username.isNotBlank()
    val isEmpty: Boolean get() = username.isEmpty()
    val isNotEmpty: Boolean get() = username.isNotEmpty()
    val displayName: String get() = name.ifBlank { username }
    val hasAvatar: Boolean get() = profileImageUrl.isNotBlank()
    val hasGifs: Boolean get() = gifs > 0
    val hasFollowers: Boolean get() = followers > 0

    companion object {
        val EMPTY = TopCreator()
    }
}
