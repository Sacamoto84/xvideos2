package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class MediaResponse(
    @SerialName("page") val page: Int = 0,
    @SerialName("pages") val pages: Int = 0,
    @SerialName("total") val total: Int = 0,
    @SerialName("gifs") val gifs: List<GifsInfo> = emptyList(),
    @SerialName("users") val users: List<UserInfo> = emptyList(),
    @SerialName("niches") val niches: List<NichesInfo> = emptyList(),
    @SerialName("tags") val tags: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = gifs.isEmpty() && users.isEmpty() && niches.isEmpty()
    val isNotEmpty: Boolean get() = !isEmpty
    val isFirstPage: Boolean get() = page <= 1
    val hasMorePages: Boolean get() = page < pages
    val hasGifs: Boolean get() = gifs.isNotEmpty()
    val hasUsers: Boolean get() = users.isNotEmpty()
    val hasNiches: Boolean get() = niches.isNotEmpty()
    val hasTags: Boolean get() = tags.isNotEmpty()

    companion object {
        val EMPTY = MediaResponse()
    }
}
