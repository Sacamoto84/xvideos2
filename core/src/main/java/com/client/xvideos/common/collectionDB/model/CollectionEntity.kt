package com.client.xvideos.common.collectionDB.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollectionEntity<T>(
    @SerialName("collection") val collection: String,
    @SerialName("list")       val items: List<T>
)
