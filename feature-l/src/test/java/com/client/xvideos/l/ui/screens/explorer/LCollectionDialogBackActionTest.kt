package com.client.xvideos.l.ui.screens.explorer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверка иерархии закрытия диалогов коллекций в L:
 * 1. Если открыт диалог создания новой коллекции — закрыть только его (вернуться к выбору коллекций).
 * 2. Если открыт диалог выбора коллекции — закрыть его.
 * 3. Если диалогов нет — действие NONE.
 */
class LCollectionDialogBackActionTest {

    @Test
    fun `диалог создания новой коллекции закрывается с приоритетом над диалогом выбора`() {
        assertEquals(
            LCollectionDialogBackAction.DISMISS_NEW_COLLECTION,
            resolveLCollectionDialogBackAction(
                visibleDialogCreateNew = true,
                visibleDialog = true
            )
        )

        assertEquals(
            LCollectionDialogBackAction.DISMISS_NEW_COLLECTION,
            resolveLCollectionDialogBackAction(
                visibleDialogCreateNew = true,
                visibleDialog = false
            )
        )
    }

    @Test
    fun `диалог выбора закрывается при отсутствии диалога создания`() {
        assertEquals(
            LCollectionDialogBackAction.DISMISS_COLLECTION_PICKER,
            resolveLCollectionDialogBackAction(
                visibleDialogCreateNew = false,
                visibleDialog = true
            )
        )
    }

    @Test
    fun `при отсутствии открытых диалогов возвращается NONE`() {
        assertEquals(
            LCollectionDialogBackAction.NONE,
            resolveLCollectionDialogBackAction(
                visibleDialogCreateNew = false,
                visibleDialog = false
            )
        )
    }
}
