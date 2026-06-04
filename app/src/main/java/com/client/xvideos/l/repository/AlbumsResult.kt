package com.client.xvideos.l.repository


// Типизированные результаты
sealed class AlbumResult : RepositoryResult() {
    data class Albums(val albumInfo: com.client.xvideos.l.net.AlbumInfo) : AlbumResult()
}