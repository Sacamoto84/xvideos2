package com.client.xvideos.r.ui.ui.lazyrow123

import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Что из общего состояния раздела касается конкретной ленты. Раньше каждая
 * лента собирала параметры из текста обоих поисков, и подтверждение поиска
 * пересоздавало их все: лайки, коллекции и подписки теряли позицию прокрутки.
 */
class LazyRow123FeedRulesTest {

    @Test
    fun `текст поиска доходит только до ленты TOP`() {
        assertEquals("abc", pagerSearchQuery(TypePager.TOP, "  abc "))

        for (type in TypePager.entries - TypePager.TOP) {
            assertEquals("лента $type пересоздалась бы от чужого поиска", "", pagerSearchQuery(type, "abc"))
        }
    }

    @Test
    fun `блок-лист действует на сетевые ленты и не трогает сохранённое`() {
        val filtered = TypePager.entries.filter { it.appliesBlockList() }.toSet()

        assertEquals(
            setOf(TypePager.TOP, TypePager.NICHES, TypePager.PROFILE, TypePager.SUBSCRIPTIONS),
            filtered,
        )
    }
}
