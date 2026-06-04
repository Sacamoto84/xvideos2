package com.client.xvideos.common.eventBus

import com.client.xvideos.common.snackbar.UiMessage

/**
 * Отправка сообщения
 * ```kotlin
 * EventBus.postEvent(Event.ShowSnackBar(UiMessage.Info(message)))
 * ```
 */
sealed class Event {
    data class Log(val message: String) : Event()

    object ArchiveCountIncrement : Event()


    /**
     * Показ снекбара с текстом из UiMessage
     */
    data class ShowSnackBar(val message: UiMessage) : Event()

    data class X_FullScreenExitPosition(val position: Long) : Event()

}
