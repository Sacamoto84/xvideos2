package com.client.xvideos.x.screens.rubric.model

import androidx.compose.runtime.Immutable

/**
 * Значение одного фильтра выдачи категории.
 *
 * @property segment Сегмент пути сайта (`s:rating`); `null` — значение по умолчанию, без сегмента.
 * @property title Подпись в приложении.
 */
sealed interface XRubricFilterOption {
    val segment: String?
    val title: String
}

/** Сортировка; по умолчанию сайт сортирует по актуальности. */
enum class XRubricSort(override val segment: String?, override val title: String) : XRubricFilterOption {
    RELEVANCE(null, "Актуальность"),
    NEWEST("s:uploaddate", "Самые свежие"),
    RATING("s:rating", "Рейтинг"),
    LENGTH("s:length", "Длина"),
    VIEWS("s:views", "Просмотров"),
}

/** Период загрузки. `m:today` сайт подписывает «Последние 3 дня». */
enum class XRubricPeriod(override val segment: String?, override val title: String) : XRubricFilterOption {
    ANY(null, "В любое время"),
    DAYS_3("m:today", "Последние 3 дня"),
    WEEK("m:week", "Эта неделя"),
    MONTH("m:month", "Этот месяц"),
    MONTHS_3("m:3month", "Последние 3 месяца"),
    MONTHS_6("m:6month", "Последние 6 месяцев"),
}

/** Длительность ролика. */
enum class XRubricDuration(override val segment: String?, override val title: String) : XRubricFilterOption {
    ANY(null, "Любая длина"),
    MIN_1_3("d:1-3min", "1–3 мин"),
    MIN_3_10("d:3-10min", "3–10 мин"),
    MIN_10_20("d:10-20min", "10–20 мин"),
    MIN_20_PLUS("d:20min_more", "20+ мин"),
}

/** Качество ролика. */
enum class XRubricQuality(override val segment: String?, override val title: String) : XRubricFilterOption {
    ANY(null, "Любое качество"),
    HD("q:hd", "720p+"),
    FULL_HD("q:1080P", "1080p+"),
}

/** Группа фильтров: кнопка панели и её выпадающий список. */
enum class XRubricFilterGroup(val title: String, val options: List<XRubricFilterOption>) {
    SORT("Сортировка", XRubricSort.entries),
    PERIOD("Период", XRubricPeriod.entries),
    DURATION("Длительность", XRubricDuration.entries),
    QUALITY("Качество", XRubricQuality.entries);

    /** Выбранное в [filters] значение этой группы. */
    fun selectedIn(filters: XRubricFilters): XRubricFilterOption = when (this) {
        SORT -> filters.sort
        PERIOD -> filters.period
        DURATION -> filters.duration
        QUALITY -> filters.quality
    }
}

/** Выбранные фильтры выдачи категории; по умолчанию — выдача сайта без фильтров. */
@Immutable
data class XRubricFilters(
    val sort: XRubricSort = XRubricSort.RELEVANCE,
    val period: XRubricPeriod = XRubricPeriod.ANY,
    val duration: XRubricDuration = XRubricDuration.ANY,
    val quality: XRubricQuality = XRubricQuality.ANY,
) {
    /** Те же фильтры, но в группе [option] выбрано [option]. */
    fun with(option: XRubricFilterOption): XRubricFilters = when (option) {
        is XRubricSort -> copy(sort = option)
        is XRubricPeriod -> copy(period = option)
        is XRubricDuration -> copy(duration = option)
        is XRubricQuality -> copy(quality = option)
    }

    /** Сегменты пути перед `<slug>-<id>`: всегда в порядке сайта s, m, d, q, со `/` после каждого. */
    val pathPrefix: String
        get() = listOf(sort, period, duration, quality)
            .mapNotNull { it.segment }
            .joinToString("") { "$it/" }
}
