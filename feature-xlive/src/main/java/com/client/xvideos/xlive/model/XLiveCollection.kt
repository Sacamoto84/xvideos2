package com.client.xvideos.xlive.model

/**
 * Подборка комнат вкладки «Эфиры» — ряд над нижними вкладками.
 *
 * @property params параметры списка комнат, которые включают подборку.
 * @property title подпись подборки.
 */
enum class XLiveCollection(val params: List<Pair<String, String>>, val title: String) {
    POPULAR(emptyList(), "Популярные"),
    NEW(listOf("new_cams" to "true"), "Новые"),
    RANDOM(listOf("sort" to "rand"), "Случайные"),
}
