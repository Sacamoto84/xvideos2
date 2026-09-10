package com.client.xvideos.l.net

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails

internal const val L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION = 1
internal const val L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

internal data class LAlbumBundleCache(
    val schemaVersion: Int,
    val cachedAtMs: Long,
    val album: AlbumDetails,
    val totalPages: Int?,
    val pics: List<PicsDetails>
)
