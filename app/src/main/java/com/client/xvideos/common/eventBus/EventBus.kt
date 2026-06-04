package com.client.xvideos.common.eventBus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Отправка сообщения
 * ```kotlin
 * EventBus.postEvent(Event.ShowSnackBar(UiMessage.Info(message)))
 * ```
 *
 * Подписывание на шину
 * ```kotlin
 * scope.launch {
 *   EventBus.events.collect { event ->
 *     if (event is Event.Log) {
 *       saveLogToFile(event.message)
 *     }
 *   }
 * }
 * ```
 */
object EventBus {
    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 1024
    )
    val events = _events.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun postEvent(event: Event) {
        Timber.i("!!! ~~~ EventBus.postEvent $event")
        scope.launch { _events.emit(event) }
    }

}
