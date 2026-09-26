package com.client.xvideos.l.model.enum

import kotlinx.serialization.Serializable

/**
 * Группа filters
 *
 * { name  : "content_id", value :  "2" }
 *
 * All 0
 * Hentai "2"
 * Non-Erotic 5
 * Real People 6
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

    companion object {
        val DEFAULT = All
        fun fromValueOrNull(value: Int?): ContentId? =
            if (value != null) entries.firstOrNull { it.value == value } else null

        fun fromValue(value: Int?, default: ContentId = DEFAULT): ContentId =
            fromValueOrNull(value) ?: default

        fun fromStringOrNull(value: String?): ContentId? =
            value?.toIntOrNull()?.let { fromValueOrNull(it) }
    }
}
