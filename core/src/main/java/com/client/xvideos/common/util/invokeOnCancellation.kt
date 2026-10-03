package com.client.xvideos.common.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Зовёт [onCancel], как только scope отменён, не дожидаясь завершения его тела.
 *
 * `Job.invokeOnCompletion` для этого не годится: он срабатывает по завершении
 * job, а тело, запертое в блокирующем вызове (сетевое чтение), завершиться не
 * может — получался хук, который при отмене не вызывался никогда, и отмена
 * ждала таймаута чтения. Наблюдатель на [Dispatchers.Unconfined] возобновляется
 * прямо в потоке, который отменяет, и не зависит от занятого диспетчера scope.
 *
 * [onCancel] выполняется в потоке отменяющего: он должен быть коротким и
 * потокобезопасным (закрыть сокет, отменить вызов). Если scope уже отменён,
 * [onCancel] вызывается сразу.
 *
 * Наблюдатель — дочерняя корутина scope, поэтому ручку обязательно снимать в
 * `finally`: иначе scope не завершится.
 *
 * @return ручка; после [DisposableHandle.dispose] [onCancel] не вызывается.
 */
fun CoroutineScope.invokeOnCancellation(onCancel: () -> Unit): DisposableHandle {
    val disposed = AtomicBoolean(false)
    val watcher = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            if (!disposed.get()) onCancel()
        }
    }
    return DisposableHandle {
        disposed.set(true)
        watcher.cancel()
    }
}
