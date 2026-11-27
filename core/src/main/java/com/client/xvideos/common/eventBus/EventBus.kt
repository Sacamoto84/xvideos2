package com.client.xvideos.common.eventBus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

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

    val subscriberCount: Int get() = _events.subscriptionCount.value
    val hasSubscribers: Boolean get() = _events.subscriptionCount.value > 0

    /**
     * Однопоточный диспетчер: корутины на нём выполняются строго по очереди,
     * поэтому подписчики видят события в том же порядке, в каком их отправили.
     *
     * Раньше здесь был обычный `Dispatchers.IO`, и каждый [postEvent] стартовал
     * свою корутину на пуле потоков — два подряд отправленных события могли
     * доехать в обратном порядке (например, прогресс приёма P2P прыгал назад).
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val queueLock = Any()
    private val overflowQueue = ArrayDeque<Event>()
    private val draining = AtomicBoolean(false)

    fun postEvent(event: Event) {
        Timber.i("!!! ~~~ EventBus.postEvent ${event.forLog()}")
        synchronized(queueLock) {
            if (overflowQueue.isEmpty() && _events.tryEmit(event)) return
            overflowQueue.addLast(event)
            if (draining.compareAndSet(false, true)) {
                scope.launch { drainOverflow() }
            }
        }
    }

    private suspend fun drainOverflow() {
        try {
            while (true) {
                val next = synchronized(queueLock) { overflowQueue.removeFirstOrNull() }
                    ?: break
                _events.emit(next)
            }
        } finally {
            draining.set(false)
            val hasMore = synchronized(queueLock) { overflowQueue.isNotEmpty() }
            if (hasMore && draining.compareAndSet(false, true)) {
                scope.launch { drainOverflow() }
            }
        }
    }

    /** Текст снекбара в журнал не идёт: в нём бывает сообщение сетевой ошибки вместе с хостом. */
    private fun Event.forLog(): String =
        if (this is Event.ShowSnackBar) "ShowSnackBar(${message.javaClass.simpleName})" else toString()

}
