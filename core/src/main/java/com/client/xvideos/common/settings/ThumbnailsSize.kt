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
         * Get all available display names
         */
        val displayNames: List<String> = entries.map { it.displayName }
    }
}
