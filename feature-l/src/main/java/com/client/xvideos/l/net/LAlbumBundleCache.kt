package com.client.xvideos.l.net

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import kotlinx.serialization.Serializable

internal const val L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION = 1
internal const val L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

@Serializable
internal data class LAlbumBundleCache(
    val schemaVersion: Int = L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION,
    val cachedAtMs: Long = 0L,
    val album: AlbumDetails = AlbumDetails(),
    val totalPages: Int? = null,
    val pics: List<PicsDetails> = emptyList()
)
