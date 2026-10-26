package com.client.xvideos.common.videoplayer.local.model

/**
 * Прятать ли панель управления по таймеру: только на весь экран, пока видео
 * играет, меню закрыто и палец не ведёт ползунок — иначе посреди
 * перетаскивания пропали бы полоса и превью кадра.
 */
fun shouldAutoHideControls(
    isFullScreen: Boolean,
    areControlsVisible: Boolean,
    isPaused: Boolean,
    isMenuExpanded: Boolean,
    isScrubbing: Boolean,
): Boolean = isFullScreen && areControlsVisible && !isPaused && !isMenuExpanded && !isScrubbing
