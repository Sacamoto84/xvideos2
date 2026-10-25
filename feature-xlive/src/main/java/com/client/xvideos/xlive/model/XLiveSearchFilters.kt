package com.client.xvideos.xlive.model

/**
 * Возраст в поиске — те же диапазоны, что у разделов сайта по возрасту.
 *
 * @property from значение `from_age`; `null` — без отбора.
 * @property to значение `to_age`; верхняя граница не включается.
 */
enum class XLiveAgeRange(val from: Int?, val to: Int?, val title: String) {
    ANY(null, null, "Любой"),
    AGE_18_19(18, 20, "18–19"),
    AGE_18_21(18, 22, "18–21"),
    AGE_20_30(20, 31, "20–30"),
    AGE_30_50(30, 51, "30–50"),
    AGE_50_PLUS(50, 100, "50+"),
}

/**
 * Регион в поиске.
 *
 * @property code значение `regions`; пустое — без отбора.
 */
enum class XLiveRegion(val code: String, val title: String) {
    ANY("", "Любой"),
    NORTH_AMERICA("NA", "Северная Америка"),
    SOUTH_AMERICA("SA", "Южная Америка"),
    EUROPE_RUSSIA("ER", "Европа и Россия"),
    ASIA("AS", "Азия"),
    OTHER("O", "Другие"),
}

/**
 * Цена привата в токенах за минуту — группы разделов сайта.
 *
 * Сервер принимает в `private_prices` список точных цен через запятую и
 * отвечает 400 на цену, которой нет в его списке. Группа «90+» — все цены от
 * 90, что встречались в списке комнат.
 *
 * @property code значение `private_prices`; пустое — без отбора.
 */
enum class XLivePrivatePrice(val code: String, val title: String) {
    ANY("", "Любая"),
    PRICE_6("6", "6"),
    PRICE_12_18("12,18", "12–18"),
    PRICE_30_42("30,42", "30–42"),
    PRICE_60_72("60,72", "60–72"),
    PRICE_90_PLUS("90,120,150,180,240,360,480,720,960,1920", "90+"),
}

/** Фильтры поиска: возраст, регион и цена привата. Пол — общий переключатель раздела. */
data class XLiveSearchFilters(
    val age: XLiveAgeRange = XLiveAgeRange.ANY,
    val region: XLiveRegion = XLiveRegion.ANY,
    val price: XLivePrivatePrice = XLivePrivatePrice.ANY,
) {
    /** Ни один фильтр не выбран. */
    val isEmpty: Boolean get() = this == XLiveSearchFilters()

    /** Параметры списка комнат для выбранных фильтров. */
    val params: List<Pair<String, String>>
        get() = buildList {
            age.from?.let { add("from_age" to "$it") }
            age.to?.let { add("to_age" to "$it") }
            if (region.code.isNotEmpty()) add("regions" to region.code)
            if (price.code.isNotEmpty()) add("private_prices" to price.code)
        }
}
