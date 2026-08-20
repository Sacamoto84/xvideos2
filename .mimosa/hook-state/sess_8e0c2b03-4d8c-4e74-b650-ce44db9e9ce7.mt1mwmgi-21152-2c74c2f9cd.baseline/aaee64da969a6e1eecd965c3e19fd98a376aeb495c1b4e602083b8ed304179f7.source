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
 * Ждать обязательно перед первым обращением к `AppPath.p2p_inbox`,
 * `p2p_outbox` и `l_cacheDownload`: уборка их рекурсивно удаляет и создаёт
 * заново, и работа с ними параллельно с этим потеряет файлы.
 */
@Singleton
class StorageCleanupGate @Inject constructor() {

    @Volatile
    private var job: Job? = null

    /**
     * Запускает уборку в [scope]. Повторный вызов игнорируется: уборка разовая
     * и на процесс одна.
     */
    @Synchronized
    fun start(scope: CoroutineScope, block: suspend () -> Unit) {
        if (job != null) return
        job = scope.launch {
            runCatching { block() }
                .onFailure { Timber.e(it, "StorageCleanupGate: уборка staging-папок упала") }
        }
    }

    /**
     * Ждёт завершения уборки. Возвращается сразу, если она не запускалась —
     * так выглядит процесс без `App.onCreate` (unit-тесты, Compose Preview).
     *
     * Падение самой уборки ожидающего не роняет: контракт — «уборка больше не
     * идёт», а не «уборка удалась». Ошибка уже в журнале.
     */
    suspend fun await() {
        job?.join()
    }
}
