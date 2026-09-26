package com.client.xvideos.r.model.tag

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TagsResponse(
    @SerialName("tags") val tags: List<TagInfo> = emptyList()
) {
    val isEmpty: Boolean get() = tags.isEmpty()
    val isNotEmpty: Boolean get() = tags.isNotEmpty()
    val size: Int get() = tags.size
    val count: Int get() = tags.size
    val firstOrNull: TagInfo? get() = tags.firstOrNull()

    companion object {
        val EMPTY = TagsResponse()
    }
}
