package com.client.xvideos.r.model

/**
 * Варианты сортировки контента в API RedGifs (для лент, поиска, профилей и ниш).
 *
 * @property value Строковое значение параметра `order`, принимаемое бэкендом RedGifs.
 */
enum class Order(val value: String) {
    /** Трендовый контент. */
    TRENDING("trending"),
    /** Топ за всё время (order=top). */
    TOP("top"),
    /** Сначала новые (свежие). */
    LATEST("latest"),
    /** Сначала старые. */
    OLDEST("oldest"),
    // Были RECENT("recent") и BEST("best"). Ни один адрес RedGifs их не
    // принимает: /v2/gifs/search отвечает 400 BadOrder, профильный адрес молча
    // игнорирует и отдаёт выдачу в своём порядке. Ни в одном наборе сортировок
    // они не стояли — только подписи в SortByOrder. Проверено 06.08.2026,
    // таблица в docs/redgifs-api.md.
    /** Топ за последний месяц (28 дней). */
    TOP28("top28"),

    /** Релевантность запросу. Есть только у поиска, у лент смысла не имеет. */
    RELEVANT("score"),

    //NEW("new"),

    /** Временное состояние сброса для принудительной перезагрузки страницы пагинации. */
    FORCE_TEMP(""),

    // Значения именно top7/top28: столько же зашито в путь у getTopThisWeek и
    // getTopThisMonth. Раньше здесь стояли "week"/"month" — они никуда не
    // уходили, потому что для лент метод выбирается по самой константе, а не
    // по её значению. Но у поиска order берётся отсюда, и с "week" сервер
    // отдавал не то.
    /** Топ за неделю (7 дней). */
    TOP_WEEK("top7"),
    /** Топ за месяц (28 дней). */
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
    /** Сортировка ниш: по числу подписчиков (убывание). */
    NICHES_SUBSCRIBERS_D("subscribers"),

    /** Сортировка ниш: по числу подписчиков (возрастание, клиентская). */
    NICHES_SUBSCRIBERS_A("subscribers"),

    /** Сортировка ниш: по числу постов (убывание). */
    NICHES_POST_D("posts"),

    /** Сортировка ниш: по числу постов (возрастание, клиентская). */
    NICHES_POST_A("posts"),

    /** Сортировка ниш: по имени от А до Я (клиентская). */
    NICHES_NAME_A_Z("name"),

    /** Сортировка ниш: по имени от Я до А (клиентская). */
    NICHES_NAME_Z_A("name");

    /** Проверяет, относится ли данная сортировка к каталогу ниш. */
    val isNichesOrder: Boolean get() = this.name.startsWith("NICHES_")

    /** Проверяет, выбрана ли сортировка LATEST. */
    val isLatest: Boolean get() = this == LATEST

    /** Проверяет, выбрана ли сортировка OLDEST. */
    val isOldest: Boolean get() = this == OLDEST

    /** Проверяет, выбрана ли сортировка TOP. */
    val isTop: Boolean get() = this == TOP

    /** Проверяет, выбрана ли сортировка TRENDING. */
    val isTrending: Boolean get() = this == TRENDING

    /** Проверяет, выбрана ли сортировка по релевантности RELEVANT. */
    val isRelevant: Boolean get() = this == RELEVANT

    /** Проверяет, выбрана ли сортировка TOP_WEEK. */
    val isTopWeek: Boolean get() = this == TOP_WEEK

    /** Проверяет, выбрана ли сортировка за месяц (TOP_MONTH или TOP28). */
    val isTopMonth: Boolean get() = this == TOP_MONTH || this == TOP28

    /** Проверяет, является ли сортировка служебным флагом сброса FORCE_TEMP. */
    val isForceTemp: Boolean get() = this == FORCE_TEMP

    /** Переход к следующему варианту сортировки циклически. */
    fun next(): Order {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущему варианту сортировки циклически. */
    fun prev(): Order {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        /** Сортировка по умолчанию. */
        val DEFAULT = LATEST

        /** Список всех имен вариантов сортировки. */
        val allNames: List<String> = entries.map { it.name }

        /** Список всех сетевых строковых значений сортировок. */
        val allValues: List<String> = entries.map { it.value }

        /** Поиск [Order] по порядковому номеру или дефолт. */
        fun fromOrdinalOrDefault(ordinal: Int, default: Order = DEFAULT): Order =
            entries.getOrNull(ordinal) ?: default

        /** Поиск [Order] по строковому значению [value] (без учета регистра). */
        fun fromValue(value: String): Order? = entries.firstOrNull { it.value.equals(value, ignoreCase = true) }

        /** Поиск [Order] по значению либо возврат [default]. */
        fun fromValueOrDefault(value: String?, default: Order = DEFAULT): Order =
            if (value != null) fromValue(value) ?: default else default

        /** Поиск [Order] по имени константы (без учета регистра). */
        fun fromNameOrNull(name: String?): Order? =
            if (name != null) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        /** Поиск [Order] по имени константы либо возврат [default]. */
        fun fromNameOrDefault(name: String?, default: Order = DEFAULT): Order =
            fromNameOrNull(name) ?: default

        /** Поиск [Order] по строковому значению [value] или имени константы. */
        fun fromStringOrNull(value: String?): Order? =
            if (value != null) fromValue(value) ?: fromNameOrNull(value) else null
    }
}

/**
 * Тип медиаконтента для фильтрации запросов в RedGifs.
 *
 * @property value Сетевой код типа медиаконтента.
 */
enum class MediaType(val value: String) {
    /** Только статические изображения (`i`). */
    IMAGE("i"),
    /** Только анимированные ролики/гифки (`g`). */
    GIF("g"),
    /** Все типы медиаконтента (`all`). */
    ALL("all");

    /** Проверяет, выбран ли тип ALL. */
    val isAll: Boolean get() = this == ALL

    /** Проверяет, выбран ли тип IMAGE. */
    val isImage: Boolean get() = this == IMAGE

    /** Проверяет, выбран ли тип GIF. */
    val isGif: Boolean get() = this == GIF

    /** Переход к следующему типу медиа циклически. */
    fun next(): MediaType {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущему типу медиа циклически. */
    fun prev(): MediaType {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        /** Тип по умолчанию (ALL). */
        val DEFAULT = ALL

        /** Список всех имен типов медиа. */
        val allNames: List<String> = entries.map { it.name }

        /** Список всех сетевых кодов типов медиа. */
        val allValues: List<String> = entries.map { it.value }

        /** Поиск [MediaType] по порядковому номеру или дефолт. */
        fun fromOrdinalOrDefault(ordinal: Int, default: MediaType = DEFAULT): MediaType =
            entries.getOrNull(ordinal) ?: default

        /** Находит [MediaType] по значению или null. */
        fun fromValueOrNull(value: String?): MediaType? =
            if (value != null) entries.firstOrNull { it.value.equals(value, ignoreCase = true) } else null

        /** Находит [MediaType] по значению или возвращает [default]. */
        fun fromValue(value: String?, default: MediaType = DEFAULT): MediaType =
            fromValueOrNull(value) ?: default

        /** Находит [MediaType] по имени константы или null. */
        fun fromNameOrNull(name: String?): MediaType? =
            if (name != null) entries.firstOrNull { it.name.equals(name, ignoreCase = true) } else null

        /** Находит [MediaType] по имени константы или возвращает [default]. */
        fun fromName(name: String?, default: MediaType = DEFAULT): MediaType =
            fromNameOrNull(name) ?: default
    }
}

/** Список запасных вариантов при отсутствии RELEVANT в доступном наборе. */
private val RELEVANT_FALLBACKS = listOf(Order.TOP, Order.TRENDING)
/** Список запасных вариантов при отсутствии TOP в доступном наборе. */
private val TOP_FALLBACKS = listOf(Order.TOP_WEEK, Order.TRENDING)

/**
 * Выбирает ближайшую подходящую сортировку из переданного [list] к текущей.
 *
 * Необходимо там, где набор допустимых сортировок динамически меняется на экране:
 * у ленты гифок он один без поиска и другой с поиском. Без этой функции
 * выбранная пользователем сортировка сбрасывалась на жесткий дефолт (Latest).
 *
 * @param list Список доступных на данном экране вариантов сортировки.
 * @return Ближайший подходящий [Order].
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
