package com.client.xvideos.l.model.enum

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
enum class ContentId(val value: Int) {
    All(0),
    Hentai(2),
    NonErotic(5),
    RealPeople(6)
}