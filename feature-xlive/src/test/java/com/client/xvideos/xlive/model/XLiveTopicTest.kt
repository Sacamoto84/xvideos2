package com.client.xvideos.xlive.model

import org.junit.Assert.assertEquals
import org.junit.Test

class XLiveTopicTest {

    @Test
    fun `теги отдельно, текст без них`() {
        assertEquals(
            XLiveTopic(
                text = "toe suck 💕 [0 tokens remaining]",
                tags = listOf("naturalredhead", "new", "teen"),
            ),
            splitRoomTitle("toe suck 💕 #naturalredhead #new #teen  [0 tokens remaining]")
        )
    }

    @Test
    fun `повторы тегов без учёта регистра отбрасываются`() {
        assertEquals(listOf("Asian", "milf"), splitRoomTitle("#Asian #milf #asian").tags)
    }

    @Test
    fun `только теги — пустой текст`() {
        assertEquals(XLiveTopic("", listOf("a", "b_c")), splitRoomTitle("#a #b_c"))
    }

    @Test
    fun `без тегов — текст как есть`() {
        assertEquals(XLiveTopic("goal: 100 tk", emptyList()), splitRoomTitle("  goal: 100 tk "))
    }

    @Test
    fun `разделитель перед тегами уходит вместе с ними, внутри текста остаётся`() {
        assertEquals("kiss with Francy !", splitRoomTitle("kiss with Francy ! - #bdsm #french").text)
        assertEquals("goal - 100 tk", splitRoomTitle("#new | goal - 100 tk ·").text)
    }

    @Test
    fun `одиночная решётка — не тег`() {
        assertEquals(XLiveTopic("# 1 goal", emptyList()), splitRoomTitle("# 1 goal"))
    }
}
