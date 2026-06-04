package com.redgifs.common.video

// Интерфейс для внешних действий (можно вынести в отдельный файл)
interface PlayerControls {
    fun forward(seconds: Float)
    fun rewind(seconds: Float)
    fun seekTo(positionSeconds: Float)
    fun stop()
    fun pause()
    fun play()
}