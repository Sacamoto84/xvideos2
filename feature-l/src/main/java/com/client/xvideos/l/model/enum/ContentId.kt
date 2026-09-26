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

    companion object {
        val DEFAULT = All
        fun fromValue(value: Int): ContentId = entries.firstOrNull { it.value == value } ?: DEFAULT
    }
}
