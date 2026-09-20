package com.client.xvideos.r.ui.root

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверка иерархии закрытия диалогов в корне экрана Red:
 * 1. Если открыт диалог ввода новой коллекции — закрыть только его (вернуться к выбору коллекций).
 * 2. Если открыт диалог выбора коллекции — закрыть его.
 * 3. Если открыт диалог блокировки — закрыть его.
 * 4. Если диалогов нет — действие NONE.
 */
class RedRootDialogBackActionTest {

    @Test
    fun `диалог создания новой коллекции закрывается с высшим приоритетом`() {
        assertEquals(
            RedRootDialogBackAction.DISMISS_NEW_COLLECTION,
            resolveRedRootDialogBackAction(
                visibleDialogCreateNew = true,
                visibleDialog = true,
                blockVisibleDialog = false
            )
        )

        assertEquals(
            RedRootDialogBackAction.DISMISS_NEW_COLLECTION,
            resolveRedRootDialogBackAction(
                visibleDialogCreateNew = true,
                visibleDialog = false,
                blockVisibleDialog = true
            )
        )
    }

    @Test
    fun `диалог выбора коллекции закрывается при отсутствии диалога создания новой коллекции`() {
        assertEquals(
            RedRootDialogBackAction.DISMISS_COLLECTION_PICKER,
            resolveRedRootDialogBackAction(
                visibleDialogCreateNew = false,
                visibleDialog = true,
                blockVisibleDialog = false
            )
        )

        assertEquals(
            RedRootDialogBackAction.DISMISS_COLLECTION_PICKER,
            resolveRedRootDialogBackAction(
                visibleDialogCreateNew = false,
                visibleDialog = true,
                blockVisibleDialog = true
            )
        )
    }

    @Test
    fun `диалог блокировки закрывается при отсутствии диалогов коллекций`() {
        assertEquals(
            RedRootDialogBackAction.DISMISS_BLOCK,
            resolveRedRootDialogBackAction(
                visibleDialogCreateNew = false,
                visibleDialog = false,
                blockVisibleDialog = true
            )
        )
    }

    @Test
    fun `при отсутствии диалогов возвращается NONE`() {
        assertEquals(
            RedRootDialogBackAction.NONE,
            resolveRedRootDialogBackAction(
                visibleDialogCreateNew = false,
                visibleDialog = false,
                blockVisibleDialog = false
            )
        )
    }
}
