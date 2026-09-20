package com.client.xvideos.x.screens.tags

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверка иерархии возврата («Назад») на экране тегов:
 * 1. Если список прокручен — прокрутить список к началу.
 * 2. Если открыта страница > 0 — вернуться на нулевую страницу пейджера.
 * 3. Если находимся на странице 0 в начале списка — закрыть экран.
 */
class TagsBackActionTest {

    @Test
    fun `иерархия возврата отдает приоритет скроллу списка перед сменой страницы и выходом`() {
        // 1. Список прокручен -> возврат к началу списка даже на странице > 0
        assertEquals(
            TagsBackAction.SCROLL_LIST_TOP,
            resolveTagsBackAction(isListScrolled = true, currentPage = 5)
        )
        assertEquals(
            TagsBackAction.SCROLL_LIST_TOP,
            resolveTagsBackAction(isListScrolled = true, currentPage = 0)
        )

        // 2. Список вверху, но открыта страница > 0 -> возврат на страницу 0
        assertEquals(
            TagsBackAction.SCROLL_PAGE_ZERO,
            resolveTagsBackAction(isListScrolled = false, currentPage = 3)
        )
        assertEquals(
            TagsBackAction.SCROLL_PAGE_ZERO,
            resolveTagsBackAction(isListScrolled = false, currentPage = 1)
        )

        // 3. Список вверху и открыта страница 0 -> закрытие экрана (pop)
        assertEquals(
            TagsBackAction.POP,
            resolveTagsBackAction(isListScrolled = false, currentPage = 0)
        )
    }
}
