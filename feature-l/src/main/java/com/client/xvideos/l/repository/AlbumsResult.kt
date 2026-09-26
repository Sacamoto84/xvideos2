package com.client.xvideos.l.repository

/**
 * Иерархия типизированных результатов репозитория Luscious для альбомов.
 */
sealed class AlbumResult : RepositoryResult() {
    /** Пустой результат без информации об альбоме. */
    data object Empty : AlbumResult()

    /**
     * Успешный результат получения информации об альбоме [albumInfo].
     *
     * @property albumInfo Распарсенные метаданные альбома Luscious.
     */
    data class Albums(val albumInfo: com.client.xvideos.l.net.AlbumInfo) : AlbumResult()
}

/**
 * Безопасно извлекает объект [com.client.xvideos.l.net.AlbumInfo], если результат является [AlbumResult.Albums].
 */
val AlbumResult.albumInfoOrNull: com.client.xvideos.l.net.AlbumInfo?
    get() = (this as? AlbumResult.Albums)?.albumInfo
