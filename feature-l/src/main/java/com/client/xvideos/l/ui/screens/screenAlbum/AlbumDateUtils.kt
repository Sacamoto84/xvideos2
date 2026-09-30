package com.client.xvideos.l.ui.screens.screenAlbum

import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ALBUM_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

internal fun formatEpochSeconds(seconds: Double, zoneId: ZoneId = ZoneId.systemDefault()): String? {
    if (!seconds.isFinite() || seconds <= 0.0) return null
    return try {
        Instant.ofEpochSecond(seconds.toLong())
            .atZone(zoneId)
            .format(ALBUM_DATE_FORMATTER)
    } catch (_: DateTimeException) {
        null
    }
}
