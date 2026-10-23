package com.client.xvideos.x.screens.channel.model

import com.client.xvideos.x.model.ChannelUiState

/** Что показывает экран канала целиком. */
enum class ChannelScreenPhase {
    /** Первая загрузка идёт: только индикатор по центру, без шапки-заглушки. */
    LOADING,

    /** Первая загрузка не удалась (или адрес некорректный): ошибка с повтором на весь экран. */
    ERROR,

    /** Шапка, сортировки и лента. Сбои дальнейших загрузок показывает страница пейджера. */
    CONTENT,
}

/**
 * Фаза экрана канала по [state]. Пока первая загрузка не удалась, показывать
 * нечего: имя из адреса, пустая аватарка и сортировки без ленты — лишнее.
 */
fun channelScreenPhase(state: ChannelUiState): ChannelScreenPhase = when {
    state.isLoaded -> ChannelScreenPhase.CONTENT
    state.error != null -> ChannelScreenPhase.ERROR
    else -> ChannelScreenPhase.LOADING
}
