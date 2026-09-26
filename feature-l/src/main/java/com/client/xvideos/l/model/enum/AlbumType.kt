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
    Pictures("pictures");

    val isAll: Boolean get() = this == All
    val isManga: Boolean get() = this == Manga
    val isPictures: Boolean get() = this == Pictures

    companion object {
        val DEFAULT = Pictures
        fun fromValue(value: String): AlbumType = entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}
