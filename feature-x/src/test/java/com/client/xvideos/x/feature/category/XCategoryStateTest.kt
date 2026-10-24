package com.client.xvideos.x.feature.category

import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.model.xCategoryOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class XCategoryStateTest {

    @After
    fun resetCategory() {
        XCategoryState.select(XCategory.STRAIGHT)
    }

    @Test
    fun `выбор меняет текущую категорию`() {
        XCategoryState.select(XCategory.TRANS)

        assertEquals(XCategory.TRANS, XCategoryState.current)
    }

    @Test
    fun `сохранённое имя категории разбирается`() {
        assertEquals(XCategory.GAY, xCategoryOf("GAY"))
        assertEquals(XCategory.TRANS, xCategoryOf("TRANS"))
        assertEquals(XCategory.STRAIGHT, xCategoryOf("STRAIGHT"))
    }

    /** Пустые или чужие настройки (первый запуск, старая версия) — гетеро, как на сайте по умолчанию. */
    @Test
    fun `неизвестное или пустое имя — гетеро`() {
        assertEquals(XCategory.STRAIGHT, xCategoryOf(null))
        assertEquals(XCategory.STRAIGHT, xCategoryOf(""))
        assertEquals(XCategory.STRAIGHT, xCategoryOf("shemale"))
    }
}
