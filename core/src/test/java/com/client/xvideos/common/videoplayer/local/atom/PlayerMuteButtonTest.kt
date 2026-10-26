package com.client.xvideos.common.videoplayer.local.atom

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerMuteButtonTest {

    @Test
    fun `подсказка кнопки звука говорит, что кнопка сделает`() {
        assertEquals("Включить звук", muteButtonHint(muted = true))
        assertEquals("Выключить звук", muteButtonHint(muted = false))
    }
}
