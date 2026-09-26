package com.client.xvideos.l.model.enum

/**
 * Индекс быстрого выбора категории фильтрации на экране дашборда:
 *
 * - Unselect (-1)
 * - Default (0)
 * - Manga (1)
 * - Hentai (2)
 * - Porn (3)
 *
 * @property value Числовой код индекса.
 */
enum class SelectIndex(val value: Int) {
    Unselect(-1),
    Default(0),
    Manga(1),
    Hentai(2),
    Porn(3);

    val isUnselect: Boolean get() = this == Unselect
    val isSelected: Boolean get() = this != Unselect
    val isDefault: Boolean get() = this == Default
    val isManga: Boolean get() = this == Manga
    val isHentai: Boolean get() = this == Hentai
    val isPorn: Boolean get() = this == Porn

    val title: String
        get() = when (this) {
            Unselect -> "Unselect"
            Default -> "Default"
            Manga -> "Manga"
            Hentai -> "Hentai"
            Porn -> "Porn"
        }

    companion object {
        val DEFAULT = Default

        fun fromValueOrNull(value: Int?): SelectIndex? =
            if (value != null) entries.firstOrNull { it.value == value } else null

        fun fromValue(value: Int?, default: SelectIndex = DEFAULT): SelectIndex =
            fromValueOrNull(value) ?: default

        fun fromIndexOrDefault(index: Int, default: SelectIndex = DEFAULT): SelectIndex =
            fromValue(index, default)

        fun fromNameOrNull(name: String?): SelectIndex? =
            if (name != null) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        fun fromStringOrNull(value: String?): SelectIndex? =
            value?.toIntOrNull()?.let { fromValueOrNull(it) } ?: fromNameOrNull(value)

        fun fromString(value: String?, default: SelectIndex = DEFAULT): SelectIndex =
            fromStringOrNull(value) ?: default
    }
}
