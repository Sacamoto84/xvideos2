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

    companion object {
        /**
         * Find ThumbnailsSize by its value
         */
        fun fromValue(value: String): ThumbnailsSize? = entries.find { it.value == value }

        /**
         * Find ThumbnailsSize by its display name
         */
        fun fromDisplayName(displayName: String): ThumbnailsSize? = entries.find { it.displayName == displayName }

        /**
         * Get all available display names
         */
        val displayNames: List<String> = entries.map { it.displayName }
    }
}
