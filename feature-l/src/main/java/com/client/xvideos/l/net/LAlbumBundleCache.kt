package com.client.xvideos.l.net

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import kotlinx.serialization.Serializable

internal const val L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION = 2
internal const val L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Структура кэшированного бандла альбома на локальном диске.
 *
 * Содержит полную информацию об альбоме [album], список всех картинок [pics]
 * и общее количество страниц [totalPages].
 *
 * @property schemaVersion Версия схемы кэша.
 * @property cachedAtMs Метка времени сохранения в миллисекундах.
 * @property album Метаданные альбома.
 * @property totalPages Количество страниц альбома.
 * @property pics Список картинок альбома.
 */
@Serializable
internal data class LAlbumBundleCache(
    val schemaVersion: Int = L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION,
    val cachedAtMs: Long = 0L,
    val album: AlbumDetails = AlbumDetails(),
    val totalPages: Int? = null,
    val pics: List<PicsDetails> = emptyList()
) {
    /** `true`, если кэш сохранен менее 7 дней назад. */
    val isFresh: Boolean get() = (System.currentTimeMillis() - cachedAtMs) < L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS
    val isCurrentSchema: Boolean get() = schemaVersion == L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION
    val isValid: Boolean get() = isCurrentSchema && album.id.isNotBlank()

    companion object {
        val EMPTY = LAlbumBundleCache()
    }
}
