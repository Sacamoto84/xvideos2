package com.client.xvideos.xlive.model

/**
 * Пол комнат в списке — переключатель у нижних вкладок.
 *
 * @property code значение параметра `genders` списка комнат; пустое — без отбора.
 * @property title подпись в меню переключателя.
 */
enum class XLiveGender(val code: String, val title: String) {
    ALL("", "Все"),
    FEMALE("f", "Женщины"),
    MALE("m", "Мужчины"),
    COUPLE("c", "Пары"),
    TRANS("t", "Транс"),
}
