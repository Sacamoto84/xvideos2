package com.client.xvideos.x.screens.rubric.model

import com.client.xvideos.x.urlStart
import org.junit.Assert.assertEquals
import org.junit.Test

/** Адрес страницы категории — тот же, что в ссылках фильтров и пагинации сайта. */
class RubricPageUrlTest {

    @Test
    fun `без фильтров — c, slug-id, номер страницы`() {
        assertEquals("$urlStart/c/Cat_B-12/0", buildRubricPageUrl(12, "Cat_B", 0, XRubricFilters()))
    }

    @Test
    fun `один фильтр — его сегмент перед slug`() {
        assertEquals(
            "$urlStart/c/s:rating/Cat_B-12/3",
            buildRubricPageUrl(12, "Cat_B", 3, XRubricFilters(sort = XRubricSort.RATING)),
        )
    }

    @Test
    fun `все четыре — в порядке s, m, d, q независимо от порядка выбора`() {
        val filters = XRubricFilters()
            .with(XRubricQuality.HD)
            .with(XRubricDuration.MIN_10_20)
            .with(XRubricPeriod.WEEK)
            .with(XRubricSort.VIEWS)

        assertEquals(
            "$urlStart/c/s:views/m:week/d:10-20min/q:hd/Cat_B-12/1",
            buildRubricPageUrl(12, "Cat_B", 1, filters),
        )
    }

    @Test
    fun `with меняет только свою группу`() {
        val filters = XRubricFilters(sort = XRubricSort.RATING).with(XRubricPeriod.MONTH)

        assertEquals(XRubricSort.RATING, filters.sort)
        assertEquals(XRubricPeriod.MONTH, filters.period)
    }

    @Test
    fun `возврат к значению по умолчанию убирает сегмент`() {
        assertEquals("", XRubricFilters(sort = XRubricSort.RATING).with(XRubricSort.RELEVANCE).pathPrefix)
    }

    @Test
    fun `выбранное значение группы`() {
        assertEquals(XRubricPeriod.WEEK, XRubricFilterGroup.PERIOD.selectedIn(XRubricFilters(period = XRubricPeriod.WEEK)))
    }
}
