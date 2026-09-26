package com.client.xvideos.common.settings

/**
 * Размер миниатюры: [value] хранится в настройках, [displayName] показывается
 * в списке выбора.
 *
 * Живёт рядом с настройками, а не в модели раздела: значение общее для
 * приложения, к разбору ответов Luscious отношения не имеет.
 */
enum class ThumbnailsSize(
    val value: String,
    val displayName: String
) {
    XMAX("xMax", "Large"),
    SMALL("small", "Medium"),
    LARGE_THUMBALIST("large_thumbnail", "Small");

    val isXMax: Boolean get() = this == XMAX
    val isSmall: Boolean get() = this == SMALL
    val isLargeThumbnail: Boolean get() = this == LARGE_THUMBALIST
    val isLarge: Boolean get() = this == XMAX
    val isMedium: Boolean get() = this == SMALL

    /** Возвращает следующий размер циклически. */
    fun next(): ThumbnailsSize {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Возвращает предыдущий размер циклически. */
    fun prev(): ThumbnailsSize {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        val DEFAULT = SMALL

        /**
         * Find ThumbnailsSize by its value or return DEFAULT
         */
        fun fromValueOrDefault(value: String?): ThumbnailsSize {
            if (value.isNullOrBlank()) return DEFAULT
            return fromValue(value) ?: DEFAULT
        }

        /**
         * Find ThumbnailsSize by its value
         */
        fun fromValue(value: String): ThumbnailsSize? {
            if (value.isEmpty()) return null
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }

        /**
         * Find ThumbnailsSize by its display name
         */
        fun fromDisplayName(displayName: String): ThumbnailsSize? {
            if (displayName.isEmpty()) return null
            return entries.find { it.displayName.equals(displayName, ignoreCase = true) }
        }

        /**
         * Safely finds ThumbnailsSize by ordinal or returns [default].
         */
        fun fromOrdinalOrDefault(ordinal: Int, default: ThumbnailsSize = DEFAULT): ThumbnailsSize =
            entries.getOrNull(ordinal) ?: default

        /**
         * Checks if the given string corresponds to a known ThumbnailsSize value.
         */
        fun isValidValue(value: String?): Boolean =
            !value.isNullOrBlank() && fromValue(value) != null

        /**
         * Checks if the given display name corresponds to a known ThumbnailsSize.
         */
        fun isValidDisplayName(displayName: String?): Boolean =
            !displayName.isNullOrBlank() && fromDisplayName(displayName) != null

        /**
         * Get all available display names
         */
        val displayNames: List<String> = entries.map { it.displayName }
    }
}
