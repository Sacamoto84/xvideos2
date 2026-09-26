package com.client.xvideos.l.model.enum

import kotlinx.serialization.Serializable

/**
 * Тип альбома в фильтрах Luscious.
 *
 * @property value Строковое значение для GraphQL-запроса (`all`, `manga`, `pictures`).
 */
@Serializable
enum class AlbumType(val value: String) {
    /** Все типы альбомов. */
    All("all"),
    /** Манга / комиксы. */
    Manga("manga"),
    /** Изображения и фотографии. */
    Pictures("pictures");

    val isAll: Boolean get() = this == All
    val isManga: Boolean get() = this == Manga
    val isPictures: Boolean get() = this == Pictures

    val title: String
        get() = when (this) {
            All -> "All"
            Manga -> "Manga"
            Pictures -> "Pictures"
        }

    companion object {
        val DEFAULT = Pictures

        fun fromValueOrNull(value: String?): AlbumType? =
            if (value != null) entries.firstOrNull { it.value.equals(value, ignoreCase = true) } else null

        fun fromValue(value: String?, default: AlbumType = DEFAULT): AlbumType =
            fromValueOrNull(value) ?: default

        fun fromNameOrNull(name: String?): AlbumType? =
            if (name != null) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        fun fromName(name: String?, default: AlbumType = DEFAULT): AlbumType =
            fromNameOrNull(name) ?: default

        fun fromIdOrDefault(id: String?, default: AlbumType = DEFAULT): AlbumType =
            fromValueOrNull(id) ?: fromNameOrNull(id) ?: default
    }
}
