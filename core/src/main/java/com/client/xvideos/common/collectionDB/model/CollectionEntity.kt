package com.client.xvideos.common.collectionDB.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CollectionEntity<T>(
    @SerialName("collection") val collection: String,
    @SerialName("list")       val items: List<T>
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val size: Int get() = items.size
    val isValid: Boolean get() = collection.isNotBlank()
    fun firstOrNull(): T? = items.firstOrNull()
    fun getOrNull(index: Int): T? = items.getOrNull(index)
}
