package com.client.xvideos.n.model

/**
 * Варианты сортировки галерей nhentai.
 *
 * @property queryValue Значение параметра sort для поискового запроса.
 * @property title Название сортировки на русском языке.
 */
enum class NSortType(val queryValue: String, val title: String) {
    RECENT("", "Новые"),
    POPULAR_ALL_TIME("popular", "Популярные"),
    POPULAR_WEEK("popular-week", "Топ недели"),
    POPULAR_TODAY("popular-today", "Топ дня")
}
