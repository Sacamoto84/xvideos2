package com.client.xvideos.x.screens.videoplayer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class XSeekPreviewModeTest {

    @Test
    fun `значение настройки туда и обратно`() {
        XSeekPreviewMode.entries.forEach { assertEquals(it, XSeekPreviewMode.fromSetting(it.settingValue)) }
        assertEquals(XSeekPreviewMode.FOLLOW, XSeekPreviewMode.fromSetting(0))
        assertEquals(XSeekPreviewMode.FULLSCREEN, XSeekPreviewMode.fromSetting(1))
        assertEquals(XSeekPreviewMode.FOLLOW, XSeekPreviewMode.fromSetting(7))
    }

    @Test
    fun `переключение A и B`() {
        assertEquals(XSeekPreviewMode.FULLSCREEN, XSeekPreviewMode.FOLLOW.toggled())
        assertEquals(XSeekPreviewMode.FOLLOW, XSeekPreviewMode.FULLSCREEN.toggled())
    }
}
