package com.client.xvideos.xlive.model

/**
 * Порядок таблицы тегов — ряд над нижними вкладками на вкладке «Теги».
 *
 * @property code значение параметра `sort`: `vc` — зрители, `rc` — комнаты,
 * `ht` — имя тега; минус впереди — по убыванию.
 * @property title подпись порядка.
 */
enum class XLiveTagSort(val code: String, val title: String) {
    VIEWERS("-vc", "Зрители"),
    ROOMS("-rc", "Комнаты"),
    NAME("ht", "А–Я"),
}
