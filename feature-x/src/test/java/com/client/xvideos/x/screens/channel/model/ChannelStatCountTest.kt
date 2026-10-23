package com.client.xvideos.x.screens.channel.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelStatCountTest {

    @Test
    fun `полное число сокращается до одной цифры после точки`() {
        assertEquals("720.8M", channelStatCount("720 776 005"))
        assertEquals("11.3B", channelStatCount("11 287 616 483"))
        assertEquals("1.2K", channelStatCount("1 248"))
        assertEquals("1.2K", channelStatCount("1,248"))
        assertEquals("999", channelStatCount("999"))
    }

    @Test
    fun `сокращение сайта переводится в тот же вид`() {
        assertEquals("715.9K", channelStatCount("715,9 к"))
        assertEquals("20.0K", channelStatCount("20 к"))
        assertEquals("1.5M", channelStatCount("1,5 М"))
        assertEquals("1.5M", channelStatCount("1.5M"))
        assertEquals("569.6K", channelStatCount("569.6k"))
    }

    @Test
    fun `непонятный текст показывается как есть`() {
        assertEquals("—", channelStatCount(" — "))
        assertEquals("", channelStatCount(""))
    }
}
