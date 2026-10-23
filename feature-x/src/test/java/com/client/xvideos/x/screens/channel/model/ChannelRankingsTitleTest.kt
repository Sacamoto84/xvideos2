package com.client.xvideos.x.screens.channel.model

import com.client.xvideos.x.model.ChannelRankingCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelRankingsTitleTest {

    @Test
    fun `одна категория — её название в заголовке`() {
        val rankings = listOf(ChannelRankingCategory(label = "Рейтинг канала (Гетеро)", ranks = emptyList()))

        assertEquals("Рейтинг канала (Гетеро)", channelRankingsTitle(rankings))
    }

    @Test
    fun `несколько категорий — общий заголовок`() {
        val rankings = listOf(
            ChannelRankingCategory(label = "Рейтинг канала (Гетеро)", ranks = emptyList()),
            ChannelRankingCategory(label = "Рейтинг канала (Гей)", ranks = emptyList()),
        )

        assertEquals("Рейтинг", channelRankingsTitle(rankings))
    }
}
