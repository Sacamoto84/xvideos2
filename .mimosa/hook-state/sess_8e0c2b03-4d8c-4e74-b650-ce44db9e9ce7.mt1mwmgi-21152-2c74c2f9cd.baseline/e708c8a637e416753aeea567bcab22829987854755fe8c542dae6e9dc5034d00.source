package com.client.xvideos.common.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * `launch`, который не уносит с собой всё приложение.
 *
 * Непойманное исключение в корутине — не то же самое, что непойманное
 * исключение в обычном коде: оно уходит в обработчик потока и убивает процесс.
 * Сеть отказывает штатно (DNS не разрешился, соединение оборвалось, сервер
 * ответил отказом), и на устройстве это выглядело так:
 *
 * ```
 * FATAL EXCEPTION: DefaultDispatcher-worker-10
 * java.net.UnknownHostException: Unable to resolve host "hls-cdn77.xvideos-cdn.com"
 * Suppressed: DiagnosticCoroutineContextException: [StandaloneCoroutine{Cancelling}, Dispatchers.IO]
 * ```
 *
 * Отвалился DNS, пока грузился список качеств HLS, — и приложение закрылось.
 *
 * [CancellationException] пробрасывается дальше: на ней держится отмена корутин,
 * и проглотить её значит сломать уход с экрана и отмену запросов.
 *
 * Рядом лежит [runCatchingCancellable] — он про то же самое, но отдаёт
 * [Result] вызывающему. Здесь отдавать некому: `launch` ничего не возвращает,
 * и единственный адресат отказа — журнал. Отличается и ширина сети: там
 * `Throwable`, здесь `Exception`, потому что глушить `OutOfMemoryError` и
 * прочие `Error` на границе корутины нельзя.
 *
 * @param message что именно не удалось — попадёт в журнал рядом с исключением.
 */
fun CoroutineScope.launchCatching(
    context: CoroutineContext = EmptyCoroutineContext,
    message: String,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(context) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "!!! %s", message)
    }
}
