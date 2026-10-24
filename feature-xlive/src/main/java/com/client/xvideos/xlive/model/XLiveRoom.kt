package com.client.xvideos.xlive.model

/**
 * Комната раздела XLive: статус эфира и адрес HLS-потока.
 *
 * @property status статус комнаты, как его отдаёт сайт: `public`, `offline`, `private`…
 * @property hlsUrl адрес мастер-плейлиста с коротким токеном; пустой, когда эфира нет.
 */
data class XLiveRoom(val status: String, val hlsUrl: String) {

    /** Поток можно смотреть: комната в открытом эфире и адрес выдан. */
    val isLive: Boolean get() = status == STATUS_PUBLIC && hlsUrl.isNotBlank()

    companion object {
        const val STATUS_PUBLIC = "public"
    }
}
