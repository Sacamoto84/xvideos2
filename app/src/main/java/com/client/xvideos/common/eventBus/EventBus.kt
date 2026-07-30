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

    /**
     * Однопоточный диспетчер: корутины на нём выполняются строго по очереди,
     * поэтому подписчики видят события в том же порядке, в каком их отправили.
     *
     * Раньше здесь был обычный `Dispatchers.IO`, и каждый [postEvent] стартовал
     * свою корутину на пуле потоков — два подряд отправленных события могли
     * доехать в обратном порядке (например, прогресс приёма P2P прыгал назад).
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    fun postEvent(event: Event) {
        Timber.i("!!! ~~~ EventBus.postEvent $event")
        scope.launch { _events.emit(event) }
    }

}
