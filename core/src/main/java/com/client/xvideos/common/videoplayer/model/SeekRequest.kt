package com.client.xvideos.common.videoplayer.model

/**
 * Запрос перемотки от [com.client.xvideos.common.videoplayer.host.MediaPlayerHost.seekTo].
 *
 * Номер [id] отличает запросы друг от друга: две перемотки в одну точку — два
 * запроса, и плеер выполняет оба. Пока плеер не подтвердил запрос, опрос
 * позиции его не трогает (см. `MediaPlayerHost.onPlayerPosition`).
 */
data class SeekRequest(val seconds: Float, val id: Long)
