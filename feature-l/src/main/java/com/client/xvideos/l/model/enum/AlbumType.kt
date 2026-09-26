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

    /** Переход к следующему типу циклически. */
    fun next(): AlbumType {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущему типу циклически. */
    fun prev(): AlbumType {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        val DEFAULT = Pictures

        val allTitles: List<String> = entries.map { it.title }

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

        fun fromOrdinalOrDefault(ordinal: Int, default: AlbumType = DEFAULT): AlbumType =
            entries.getOrNull(ordinal) ?: default
    }
}
