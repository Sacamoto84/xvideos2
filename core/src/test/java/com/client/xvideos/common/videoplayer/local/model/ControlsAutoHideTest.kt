package com.client.xvideos.common.videoplayer.local.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Панель управления полноэкранного плеера X сама прячется через 3,5 с. Пока
 * палец ведёт ползунок, прятаться ей нельзя: вместе с ней пропали бы полоса и
 * превью кадра.
 */
class ControlsAutoHideTest {

    @Test
    fun `панель прячется только в полном экране при игре без меню и без перетаскивания`() {
        assertTrue(shouldAutoHideControls(true, true, false, false, false))
        assertFalse(shouldAutoHideControls(true, true, false, false, true))
        assertFalse(shouldAutoHideControls(true, true, true, false, false))
        assertFalse(shouldAutoHideControls(true, true, false, true, false))
        assertFalse(shouldAutoHideControls(false, true, false, false, false))
        assertFalse(shouldAutoHideControls(true, false, false, false, false))
    }
}
