package com.client.xvideos.l.repository

/**
 * Иерархия типизированных результатов репозитория Luscious для альбомов.
 */
sealed class AlbumResult : RepositoryResult() {
    /**
     * Успешный результат получения информации об альбоме [albumInfo].
     *
     * @property albumInfo Распарсенные метаданные альбома Luscious.
     */
    data class Albums(val albumInfo: com.client.xvideos.l.net.AlbumInfo) : AlbumResult()
}
