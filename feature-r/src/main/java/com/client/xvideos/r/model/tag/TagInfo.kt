package com.client.xvideos.r.model.tag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagInfo(
    @SerialName("name") val name: String = "",
    @SerialName("count") val count: Long = 0L
)


