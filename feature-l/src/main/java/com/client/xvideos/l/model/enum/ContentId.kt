package com.client.xvideos.l.model.enum

import kotlinx.serialization.Serializable

/**
 * Идентификатор категории контента в фильтрах Luscious:
 *
 * - 0: All (все)
 * - 2: Hentai (хентай / аниме)
 * - 5: Non-Erotic (не эротика)
 * - 6: Real People (реальные люди / косплей)
 *
 * @property value Числовой ID для GraphQL-запроса.
 */
@Serializable
enum class ContentId(val value: Int) {
    All(0),
    Hentai(2),
    NonErotic(5),
    RealPeople(6);

    val isAll: Boolean get() = this == All
    val isHentai: Boolean get() = this == Hentai
    val isNonErotic: Boolean get() = this == NonErotic
    val isRealPeople: Boolean get() = this == RealPeople
    val isSpecific: Boolean get() = this != All

    val title: String
        get() = when (this) {
            All -> "All"
            Hentai -> "Hentai"
            NonErotic -> "Non-Erotic"
            RealPeople -> "Real People"
        }

    /** Переход к следующей категории контента циклически. */
    fun next(): ContentId {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущей категории контента циклически. */
    fun prev(): ContentId {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        val DEFAULT = All

        val allValues: List<Int> = entries.map { it.value }
        val allTitles: List<String> = entries.map { it.title }
        val allNames: List<String> = entries.map { it.name }

        fun fromValueOrNull(value: Int?): ContentId? =
            if (value != null) entries.firstOrNull { it.value == value } else null

        fun fromValue(value: Int?, default: ContentId = DEFAULT): ContentId =
            fromValueOrNull(value) ?: default

        fun fromNameOrNull(name: String?): ContentId? =
            if (name != null) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        fun fromStringOrNull(value: String?): ContentId? =
            value?.toIntOrNull()?.let { fromValueOrNull(it) } ?: fromNameOrNull(value)

        fun fromString(value: String?, default: ContentId = DEFAULT): ContentId =
            fromStringOrNull(value) ?: default

        fun fromIdOrDefault(id: String?, default: ContentId = DEFAULT): ContentId =
            fromString(id, default)

        fun fromOrdinalOrDefault(ordinal: Int, default: ContentId = DEFAULT): ContentId =
            entries.getOrNull(ordinal) ?: default
    }
}
