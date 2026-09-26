package com.client.xvideos.common.eventBus

import androidx.compose.runtime.Immutable
import com.client.xvideos.common.snackbar.UiMessage

/**
 * Отправка сообщения
 * ```kotlin
 * EventBus.postEvent(Event.ShowSnackBar(UiMessage.Info(message)))
 * ```
 */
@Immutable
sealed class Event {
    data class Log(val message: String) : Event()

    object ArchiveCountIncrement : Event()


    val isSnackBar: Boolean get() = this is ShowSnackBar
    val isLog: Boolean get() = this is Log
    val isExitPosition: Boolean get() = this is X_FullScreenExitPosition

    /**
     * Показ снекбара с текстом из UiMessage
     */
    data class ShowSnackBar(val message: UiMessage) : Event() {
        val isError: Boolean get() = message is UiMessage.Error
        val isSuccess: Boolean get() = message is UiMessage.Success
        val isInfo: Boolean get() = message is UiMessage.Info
        val isWarning: Boolean get() = message is UiMessage.Warning
    }

    data class X_FullScreenExitPosition(val position: Long) : Event()

    sealed class P2pTransferUpdate : Event() {
        data class Progress(val endpointName: String, val transferred: Long, val total: Long) : P2pTransferUpdate()
        data class Success(val endpointName: String) : P2pTransferUpdate()
        data class Error(val endpointName: String, val message: String) : P2pTransferUpdate()
    }
}
