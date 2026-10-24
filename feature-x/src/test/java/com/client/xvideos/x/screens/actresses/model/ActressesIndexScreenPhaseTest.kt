package com.client.xvideos.x.screens.actresses.model

import com.client.xvideos.x.model.ActressesIndexUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ActressesIndexScreenPhaseTest {

    @Test
    fun `до первой загрузки экран показывает только загрузку`() {
        val state = ActressesIndexUiState(title = "Каталог", isLoadingInitial = true)

        assertEquals(ActressesIndexScreenPhase.LOADING, actressesIndexScreenPhase(state))
    }

    @Test
    fun `сбой первой загрузки — ошибка на весь экран`() {
        val state = ActressesIndexUiState(isLoadingInitial = false, error = "Не удалось загрузить")

        assertEquals(ActressesIndexScreenPhase.ERROR, actressesIndexScreenPhase(state))
    }

    @Test
    fun `загруженный каталог показывает содержимое`() {
        val state = ActressesIndexUiState(isLoadingInitial = false, isLoaded = true)

        assertEquals(ActressesIndexScreenPhase.CONTENT, actressesIndexScreenPhase(state))
    }

    /** Смена страны или периода: фильтры остаются, загрузку показывает сетка. */
    @Test
    fun `перезагрузка после смены фильтра не прячет фильтры`() {
        val state = ActressesIndexUiState(isLoadingInitial = true, isLoaded = true)

        assertEquals(ActressesIndexScreenPhase.CONTENT, actressesIndexScreenPhase(state))
    }

    @Test
    fun `сбой после смены фильтра не прячет фильтры`() {
        val state = ActressesIndexUiState(isLoadingInitial = false, isLoaded = true, error = "Не удалось загрузить")

        assertEquals(ActressesIndexScreenPhase.CONTENT, actressesIndexScreenPhase(state))
    }
}
