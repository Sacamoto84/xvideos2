package com.client.xvideos.x.screens.channel.model

import com.client.xvideos.x.model.ChannelUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelScreenPhaseTest {

    @Test
    fun `до первой загрузки экран показывает только загрузку`() {
        assertEquals(ChannelScreenPhase.LOADING, channelScreenPhase(ChannelUiState()))
    }

    @Test
    fun `сбой первой загрузки — ошибка на весь экран`() {
        val state = ChannelUiState(error = "Не удалось загрузить")

        assertEquals(ChannelScreenPhase.ERROR, channelScreenPhase(state))
    }

    @Test
    fun `загруженный экран показывает содержимое`() {
        assertEquals(ChannelScreenPhase.CONTENT, channelScreenPhase(ChannelUiState(isLoaded = true)))
    }

    /** Смена сортировки или фильтра после первой загрузки: сбой показывает страница 0. */
    @Test
    fun `сбой после первой загрузки не прячет экран`() {
        val state = ChannelUiState(isLoaded = true, error = "Не удалось загрузить")

        assertEquals(ChannelScreenPhase.CONTENT, channelScreenPhase(state))
    }
}
