package com.client.xvideos.xlive.model

/**
 * Тег в таблице тегов.
 *
 * @property name тег без `#`.
 * @property viewers зрители во всех комнатах с тегом.
 * @property rooms комнаты с тегом.
 * @property topRooms самые популярные комнаты с тегом — до 12, с превью.
 */
data class XLiveTag(
    val name: String,
    val viewers: Int,
    val rooms: Int,
    val topRooms: List<XLiveTagRoom> = emptyList(),
)

/** Комната в превью тега: ник и снимок эфира 16:9. */
data class XLiveTagRoom(val username: String, val thumbnail: String)

/**
 * Страница таблицы тегов.
 *
 * @property total сколько всего тегов под текущим полом.
 */
data class XLiveTagPage(val tags: List<XLiveTag>, val total: Int)
