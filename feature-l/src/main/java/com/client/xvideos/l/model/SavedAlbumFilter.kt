package com.client.xvideos.l.model

import androidx.compose.runtime.Immutable
import java.io.Serializable
import java.util.UUID

@Immutable
@kotlinx.serialization.Serializable
data class SavedAlbumFilter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val filter: AlbumListFilter
) : Serializable
