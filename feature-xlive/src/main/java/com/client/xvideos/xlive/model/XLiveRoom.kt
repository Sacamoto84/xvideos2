package com.client.xvideos.xlive.model

/**
 * Комната раздела XLive: статус эфира, адрес HLS-потока и то, что видно в шапке.
 *
 * @property status статус комнаты, как его отдаёт сайт: `public`, `offline`, `private`…
 * @property hlsUrl адрес мастер-плейлиста с коротким токеном; пустой, когда эфира нет.
 * @property username ник модели, он же имя комнаты.
 * @property title тема комнаты вместе с хештегами.
 * @property viewers зрители сейчас.
 * @property gender пол от сайта: `female`, `male`, `couple`, `trans`.
 * @property startedAt начало эфира, unix-секунды; 0 — неизвестно.
 * @property ratingPercent доля положительных оценок, проценты; `null` — оценок нет.
 */
data class XLiveRoom(
    val status: String,
    val hlsUrl: String,
    val username: String = "",
    val title: String = "",
    val viewers: Int = 0,
    val gender: String = "",
    val startedAt: Long = 0L,
    val ratingPercent: Int? = null,
) {

    /** Поток можно смотреть: комната в открытом эфире и адрес выдан. */
    val isLive: Boolean get() = status == STATUS_PUBLIC && hlsUrl.isNotBlank()

    companion object {
        const val STATUS_PUBLIC = "public"
    }
}
