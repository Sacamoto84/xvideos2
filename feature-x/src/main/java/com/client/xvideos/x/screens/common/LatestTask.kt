package com.client.xvideos.x.screens.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Задачи «последняя побеждает»: новая отменяет прошлую.
 *
 * `finally` отменённой задачи выполняется уже после старта новой — когда её
 * приостановка вернётся в поток области. Сброс признака загрузки в таком
 * `finally` гасил индикатор новой задачи, поэтому о простое сообщает только
 * последняя запущенная.
 *
 * @param scope Область, в которой идут задачи.
 * @param onIdle Вызывается, когда закончилась последняя запущенная задача.
 */
internal class LatestTask(
    private val scope: CoroutineScope,
    private val onIdle: () -> Unit,
) {
    private var job: Job? = null

    fun launch(block: suspend () -> Unit) {
        job?.cancel()
        // LAZY: задача попадает в job до первого шага, и finally сверяется с ней.
        val task = scope.launch(start = CoroutineStart.LAZY) {
            val self = coroutineContext[Job]
            try {
                block()
            } finally {
                if (job === self) onIdle()
            }
        }
        job = task
        task.start()
    }
}
