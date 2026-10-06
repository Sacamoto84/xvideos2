package com.client.xvideos.l.ui.screens.screenFullScreen.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Номер картинки в полноэкранном просмотре. Метка на экране показывала индекс
 * с нуля, а окно сведений о той же картинке — номер с единицы.
 */
class LPicturePositionLabelTest {

    @Test
    fun `первая картинка подписана единицей, а не нулём`() {
        assertEquals("1 / 12", lPicturePositionLabel(position = 0, total = 12))
    }

    @Test
    fun `последняя картинка подписана общим числом`() {
        assertEquals("12 / 12", lPicturePositionLabel(position = 11, total = 12))
    }
}
