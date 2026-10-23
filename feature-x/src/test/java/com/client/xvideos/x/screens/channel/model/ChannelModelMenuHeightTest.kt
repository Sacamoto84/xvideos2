package com.client.xvideos.x.screens.channel.model

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelModelMenuHeightTest {

    @Test
    fun `короткий список — высота по числу пунктов`() {
        assertEquals(0.dp, channelModelMenuListHeight(0))
        assertEquals(48.dp, channelModelMenuListHeight(1))
        assertEquals(144.dp, channelModelMenuListHeight(3))
    }

    @Test
    fun `длинный список упирается в потолок и прокручивается внутри`() {
        assertEquals(300.dp, channelModelMenuListHeight(7))
        assertEquals(300.dp, channelModelMenuListHeight(500))
    }
}
