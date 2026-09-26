package com.client.xvideos.common.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Разовая уборка staging-папок и ожидание её завершения.
 *
 * Раньше `Job` уборки жил полем в `App`, а дождаться его можно было только
 * через `App.instance` — единственная причина, по которой этот глобал вообще
 * существовал. Теперь ожидание инжектируется как обычная зависимость.
 *
 * Ждать обязательно перед первым обращением к временным директориям кэша:
 * уборка их рекурсивно удаляет и создаёт заново, и работа с ними параллельно
 * с этим может привести к потере данных.
 */
@Singleton
class StorageCleanupGate @Inject constructor() {

    @Volatile
    private var job: Job? = null

    /**
     * Запускает уборку в [scope]. Повторный вызов игнорируется: уборка разовая
     * и на процесс одна.
     *
     * @param scope Скоуп выполнения уборки.
     * @param block Асинхронное действие очистки.
     */
    @Synchronized
    fun start(scope: CoroutineScope, block: suspend () -> Unit) {
        if (job != null) return
        job = scope.launch {
            runCatching { block() }
                .onFailure { Timber.e(it, "StorageCleanupGate: уборка staging-папок упала") }
        }
    }

    /** Истина, если процесс уборки был запущен. */
    val isStarted: Boolean get() = job != null

    /** Истина, если уборка успешно или с ошибкой завершилась (или не запускалась). */
    val isCompleted: Boolean get() = job?.isCompleted ?: true

    /** Истина, если уборка выполняется прямо сейчас. */
    val isActive: Boolean get() = job?.isActive ?: false

    /** Истина, если уборка запущена, но ещё не успела завершиться. */
    val isPending: Boolean get() = isStarted && !isCompleted

    /** Истина, если гейт находится в состоянии покоя (ещё не запускался или уже завершился). */
    val isIdle: Boolean get() = !isStarted || isCompleted

    /** Проверяет активность выполнения задачи уборки (алиас для [isActive]). */
    fun isRunning(): Boolean = isActive

    /** Сброс внутреннего состояния для изоляции в тестах. */
    fun resetForTesting() {
        job = null
    }

    /**
     * Ждёт завершения уборки. Возвращается сразу, если она не запускалась —
     * так выглядит процесс без `App.onCreate` (unit-тесты, Compose Preview).
     *
     * Падение самой уборки ожидающего не роняет: контракт — «уборка больше не
     * идёт», а не «уборка удалась». Ошибка уже в журнале.
     */
    suspend fun await() {
        val currentJob = job
        if (currentJob != null && currentJob.isActive) {
            currentJob.join()
        }
    }
}
