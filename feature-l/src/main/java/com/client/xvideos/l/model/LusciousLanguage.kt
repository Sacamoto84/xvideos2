package com.client.xvideos.l.model

import kotlinx.serialization.Serializable

/**
 * Идентификаторы языков для фильтрации альбомов на Luscious.
 *
 * @property id Числовой идентификатор языка в GraphQL API (`language_ids`).
 * @property title Отображаемое название языка.
 */
@Serializable
enum class LusciousLanguage(val id: Int, val title: String) {
    ENGLISH(1, "English"),
    PORTUGUESE(100, "Portuguese / Português"),
    THAI(101, "Thai / ไทย"),
    NO_WORDS(27, "No Words"),
    JAPANESE(2, "Japanese / 日本語"),
    SPANISH(3, "Spanish / Español"),
    ITALIAN(4, "Italian / Italiana"),
    GERMAN(5, "German / Deutsch"),
    FRENCH(6, "French / Français"),
    RUSSIAN(7, "Russian / Pусский"),
    CHINESE(8, "Chinese / 中文"),
    KOREAN(9, "Korean / 한국어"),
    OTHER(99, "Other");

    val isEnglish: Boolean get() = this == ENGLISH
    val isRussian: Boolean get() = this == RUSSIAN
    val isJapanese: Boolean get() = this == JAPANESE
    val isNoWords: Boolean get() = this == NO_WORDS

    /** Переход к следующему языку циклически. */
    fun next(): LusciousLanguage {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущему языку циклически. */
    fun prev(): LusciousLanguage {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        val DEFAULT = ENGLISH

        val allIds: List<Int> = entries.map { it.id }
        val allTitles: List<String> = entries.map { it.title }
        val allNames: List<String> = entries.map { it.name }

        fun fromIdOrNull(id: Int?): LusciousLanguage? =
            if (id != null) entries.firstOrNull { it.id == id } else null

        fun fromIdOrDefault(id: Int?, default: LusciousLanguage = DEFAULT): LusciousLanguage =
            fromIdOrNull(id) ?: default

        fun fromTitleOrNull(title: String?): LusciousLanguage? =
            if (!title.isNullOrBlank()) entries.firstOrNull { it.title.equals(title, ignoreCase = true) } else null

        fun fromTitleOrDefault(title: String?, default: LusciousLanguage = DEFAULT): LusciousLanguage =
            fromTitleOrNull(title) ?: default

        fun fromNameOrNull(name: String?): LusciousLanguage? =
            if (!name.isNullOrBlank()) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        fun fromNameOrDefault(name: String?, default: LusciousLanguage = DEFAULT): LusciousLanguage =
            fromNameOrNull(name) ?: default

        fun fromStringOrNull(value: String?): LusciousLanguage? =
            value?.toIntOrNull()?.let { fromIdOrNull(it) } ?: fromNameOrNull(value) ?: fromTitleOrNull(value)

        fun fromStringOrDefault(value: String?, default: LusciousLanguage = DEFAULT): LusciousLanguage =
            fromStringOrNull(value) ?: default

        fun fromOrdinalOrDefault(ordinal: Int, default: LusciousLanguage = DEFAULT): LusciousLanguage =
            entries.getOrNull(ordinal) ?: default
    }
}
