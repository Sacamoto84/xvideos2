package com.client.xvideos.common.snackbar

import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus

object SnackBar{
    //------------------------------------------------------------------------------
    fun info(message: String) {
        EventBus.postEvent( Event.ShowSnackBar(UiMessage.Info(message)))
    }

    fun error(message: String) {
        EventBus.postEvent( Event.ShowSnackBar(UiMessage.Error(message)))
    }

    fun success(message: String) {
        EventBus.postEvent( Event.ShowSnackBar(UiMessage.Success(message)))
    }

    fun warning(message: String) {
        EventBus.postEvent( Event.ShowSnackBar(UiMessage.Warning(message)))
    }
}
