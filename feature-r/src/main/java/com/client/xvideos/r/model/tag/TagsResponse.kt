package com.client.xvideos.r.model.tag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagsResponse(
    @SerialName("tags") val tags: List<TagInfo> = emptyList()
)
