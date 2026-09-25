package com.client.xvideos.r.model.tag

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TagInfo(
    @SerialName("name") val name: String = "",
    @SerialName("count") val count: Long = 0L
) {
    val isValid: Boolean get() = name.isNotBlank()

    companion object {
        val EMPTY = TagInfo()
    }
}


