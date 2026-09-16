package com.client.xvideos.r.model.tag

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TagsResponse(
    @SerialName("tags") val tags: List<TagInfo> = emptyList()
)
