package com.client.xvideos.r.model

enum class Order(val value: String) {
    TRENDING("trending"),
    TOP("top"),
    LATEST("latest"),
    OLDEST("oldest"),
    // Были RECENT("recent") и BEST("best"). Ни один адрес RedGifs их не
    // принимает: /v2/gifs/search отвечает 400 BadOrder, профильный адрес молча
    // игнорирует и отдаёт выдачу в своём порядке. Ни в одном наборе сортировок
    // они не стояли — только подписи в SortByOrder. Проверено 06.08.2026,
    // таблица в docs/redgifs-api.md.
    TOP28("top28"),

    /** Релевантность запросу. Есть только у поиска, у лент смысла не имеет. */
    RELEVANT("score"),

    //NEW("new"),

    FORCE_TEMP(""),

    // Значения именно top7/top28: столько же зашито в путь у getTopThisWeek и
    // getTopThisMonth. Раньше здесь стояли "week"/"month" — они никуда не
    // уходили, потому что для лент метод выбирается по самой константе, а не
    // по её значению. Но у поиска order берётся отсюда, и с "week" сервер
    // отдавал не то.
    TOP_WEEK("top7"),
    TOP_MONTH("top28"),

    // «Топ за всё время» — это [TOP]. Здесь стоял отдельный TOP_ALLTIME("alltime"),
    // но такого значения у RedGifs нет: /v2/gifs/search отвечает
    // 400 BadOrder и перечисляет набор — top, top7, top28, latest, score,
    // trending. Проверено 06.08.2026, подробности в docs/redgifs-api.md.
    //
    // Ошибки пользователь не видел: ItemTopPagingSource уводил TOP_ALLTIME в
    // else и отдавал getTopThisWeek, где order=top7 зашит в путь. То есть
    // «All time» показывал неделю.


    //NICHES
    NICHES_SUBSCRIBERS_D("subscribers"),

    NICHES_SUBSCRIBERS_A("subscribers"),

    NICHES_POST_D("posts"),

    NICHES_POST_A("posts"),
    NICHES_NAME_A_Z("name"),
    NICHES_NAME_Z_A("name");

    val isNichesOrder: Boolean get() = this.name.startsWith("NICHES_")
    val isLatest: Boolean get() = this == LATEST
    val isOldest: Boolean get() = this == OLDEST
    val isTop: Boolean get() = this == TOP
    val isTrending: Boolean get() = this == TRENDING
    val isRelevant: Boolean get() = this == RELEVANT

    companion object {
        val DEFAULT = LATEST
        fun fromValue(value: String): Order? = entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
        fun fromValueOrDefault(value: String?, default: Order = DEFAULT): Order =
            if (value != null) fromValue(value) ?: default else default
    }
}

enum class MediaType(val value: String) {
    IMAGE("i"),
    GIF("g"),
    ALL("all");

    val isAll: Boolean get() = this == ALL
    val isImage: Boolean get() = this == IMAGE
    val isGif: Boolean get() = this == GIF

    companion object {
        val DEFAULT = ALL
        fun fromValueOrNull(value: String?): MediaType? =
            if (value != null) entries.firstOrNull { it.value.equals(value, ignoreCase = true) } else null

        fun fromValue(value: String?, default: MediaType = DEFAULT): MediaType =
            fromValueOrNull(value) ?: default
    }
}

private val RELEVANT_FALLBACKS = listOf(Order.TOP, Order.TRENDING)
private val TOP_FALLBACKS = listOf(Order.TOP_WEEK, Order.TRENDING)

/**
 * Ближайшая сортировка из [list] к текущей.
 *
 * Нужно там, где набор сортировок меняется под экраном: у ленты гифок он один
 * без поиска и другой с поиском, и выбранное значение может в новом наборе
 * отсутствовать. Раньше в таком случае жёстко ставился [Order.LATEST] — то
 * есть выбор пользователя молча заменялся на самый далёкий от него: выбрал
 * «All time», начал искать — искалось по «Latest».
 *
 * Замены подобраны так, чтобы попадать только в значения, принимаемые сервером
 * (таблица в docs/redgifs-api.md).
 *
 * После сведения `TOP_ALLTIME` к [TOP] наборы ленты и поиска различаются одним
 * элементом — [RELEVANT], которого у лент нет и быть не может. Остальные ветки
 * оставлены на случай новых наборов: функция общая, её зовёт `SortByOrder` для
 * всех меню сортировки, включая профиль и ниши.
 */
fun Order.nearestIn(list: List<Order>): Order {
    if (list.isEmpty()) return this
    if (this in list) return this
    val preferred = when (this) {
        Order.RELEVANT -> RELEVANT_FALLBACKS
        Order.TOP -> TOP_FALLBACKS
        else -> emptyList()
    }
    return preferred.firstOrNull { it in list } ?: list.firstOrNull() ?: this
}











