package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CreatorResponse(
    @SerialName("gifs") val gifs: List<GifsInfo> = emptyList(),
    @SerialName("users") val users: List<UserInfo> = emptyList(),
    @SerialName("niches") val niches: List<NichesInfo> = emptyList(),
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("pages") val pages: Int = 0,
    @SerialName("total") val total: Int = 0,
) {
    val isEmpty: Boolean get() = gifs.isEmpty() && users.isEmpty() && niches.isEmpty()
    val isNotEmpty: Boolean get() = !isEmpty
    val hasMorePages: Boolean get() = page < pages

    companion object {
        val EMPTY = CreatorResponse()
    }
}
