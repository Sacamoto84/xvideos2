package com.client.xvideos.l.model.enum

import kotlinx.serialization.Serializable

/**
 * {
 *   "name": "album_type",
 *   "value": "pictures"
 * }
 */
@Serializable
enum class AlbumType(val value: String) {
    All("all"),
    Manga("manga"),
    Pictures("pictures")
}
