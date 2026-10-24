package com.client.xvideos.x.screens.actresses.model

import com.client.xvideos.x.model.ActressesIndexUiState

/** Что показывает экран рейтинга целиком. */
enum class ActressesIndexScreenPhase {
    /** Первая загрузка идёт: только индикатор по центру, без шапки и пустых фильтров. */
    LOADING,

    /** Первая загрузка не удалась: ошибка с повтором на весь экран. */
    ERROR,

    /** Шапка, фильтры и сетка. Загрузку и сбой после смены фильтра показывает сетка. */
    CONTENT,
}

/**
 * Фаза экрана рейтинга по [state]. Пока каталог ни разу не загрузился, показывать
 * нечего: кнопки фильтров без подписей и заголовок-заглушка — лишнее.
 */
fun actressesIndexScreenPhase(state: ActressesIndexUiState): ActressesIndexScreenPhase = when {
    state.isLoaded -> ActressesIndexScreenPhase.CONTENT
    state.error != null -> ActressesIndexScreenPhase.ERROR
    else -> ActressesIndexScreenPhase.LOADING
}
