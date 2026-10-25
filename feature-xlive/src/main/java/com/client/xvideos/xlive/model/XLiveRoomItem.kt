package com.client.xvideos.xlive.model

/**
 * Комната в списке «Эфиры» — то, что видно на карточке.
 *
 * @property username ник модели, он же имя комнаты.
 * @property age возраст; `null` — не указан.
 * @property gender пол от сайта: `f`, `m`, `c`, `s` (транс).
 * @property country код страны из двух букв; пустой — не указан.
 * @property viewers зрители сейчас.
 * @property subject тема комнаты.
 * @property tags теги комнаты без `#`.
 * @property thumbnail снимок эфира 16:9.
 * @property isNew новая комната.
 * @property isPromoted промо-место сайта: одно на страницу сверх лимита.
 */
data class XLiveRoomItem(
    val username: String,
    val age: Int? = null,
    val gender: String = "",
    val country: String = "",
    val viewers: Int = 0,
    val subject: String = "",
    val tags: List<String> = emptyList(),
    val thumbnail: String = "",
    val isNew: Boolean = false,
    val isPromoted: Boolean = false,
)

/**
 * Страница списка комнат.
 *
 * @property total сколько всего комнат подходит под отбор; список живой, число плавает.
 */
data class XLiveRoomPage(val rooms: List<XLiveRoomItem>, val total: Int)
