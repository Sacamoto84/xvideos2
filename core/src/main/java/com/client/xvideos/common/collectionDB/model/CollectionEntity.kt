package com.client.xvideos.common.collectionDB.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CollectionEntity<T>(
    @SerialName("collection") val collection: String,
    @SerialName("list")       val items: List<T>
)
