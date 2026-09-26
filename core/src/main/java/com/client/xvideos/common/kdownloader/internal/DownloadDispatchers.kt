package com.client.xvideos.common.kdownloader.internal

import com.client.xvideos.common.kdownloader.database.DbHelper
import com.client.xvideos.common.kdownloader.database.DownloadModel
import com.client.xvideos.common.kdownloader.Status
import com.client.xvideos.common.kdownloader.utils.getTempPath
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Координатор корутинных скоупов и диспетчеризации задач [KDownloader].
 *
 * Архитектурно разделяет потоки на три изолированных [CoroutineScope]:
 * 1. [scope] — последовательное или параллельное выполнение сетевых задач [DownloadTask] на `Dispatchers.IO`.
 * 2. [dbScope] — изолированные операции чтения/записи в локальную БД и файловые очистки, независимые от отмены сетевых задач.
 * 3. [callbackScope] — гарантированная доставка событий слушателям на главном потоке [Dispatchers.Main].
 *
 * @param dbHelper Хранилище записей о загрузках.
 */
class DownloadDispatchers(private val dbHelper: DbHelper) {

    /** Скоуп для запуска сетевых задач загрузки. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1) +
            CoroutineExceptionHandler { _, _ ->

            })

    /** Изолированный скоуп для фоновых операций с БД и файловой системой. */
    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1) +
            CoroutineExceptionHandler { _, _ ->

            })

    /** Скоуп главного потока для безопасной доставки событий в UI. */
    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main +
            CoroutineExceptionHandler { _, _ ->

            })

    /**
     * Помещает задачу в корутинный пул выполнения и привязывает сгенерированный [Job] к запросу.
     *
     * @param req Запрос на скачивание.
     * @return Идентификатор загрузки [DownloadRequest.downloadId].
     */
    fun enqueue(req: DownloadRequest): Int {
        val job = scope.launch(Dispatchers.IO.limitedParallelism(1)) {
            execute(req)
        }
        req.job = job
        return req.downloadId
    }

    /**
     * Запускает [DownloadTask] и маршрутизирует его коллбэки в главный поток.
     */
    private suspend fun execute(request: DownloadRequest) {
        DownloadTask(request, dbHelper).run(
            onStart = {
                executeOnMainThread { request.listener?.onStart() }
            },
            onProgress = {
                executeOnMainThread { request.listener?.onProgress(it) }
            },
            onPause = {
                executeOnMainThread { request.listener?.onPause() }
            },
            onCompleted = {
                executeOnMainThread {
                    request.listener?.onCompleted()
                    request.listener = null
                }
            },
            onError = {
                executeOnMainThread {
                    request.listener?.onError(it)
                    request.listener = null
                }
            }
        )
    }

    /**
     * Выполняет блок кода на главном потоке UI через [callbackScope].
     */
    private fun executeOnMainThread(block: () -> Unit) {
        // Колбэки слушателя (onStart/onProgress/onCompleted/...) доходят до UI,
        // поэтому выполняем их именно на главном потоке.
        // Используем callbackScope, чтобы cancelAll() на scope (воркеры загрузки)
        // не сбивал нотификации слушателей об отмене.
        callbackScope.launch {
            block()
        }
    }

    /**
     * Отменяет отдельную задачу, прерывает её корутину, удаляет временный файл и запись из БД.
     *
     * @param req Запрос, подлежащий отмене.
     */
    fun cancel(req: DownloadRequest) {
        val wasPaused = req.status == Status.PAUSED
        val wasQueued = req.status == Status.QUEUED
        val tempPath = if (wasPaused) getTempPath(req.dirPath, req.fileName) else null
        if (wasPaused) {
            req.reset()
        }

        req.status = Status.CANCELLED
        val job = req.job
        job?.cancel()

        val notRunning = job?.isActive != true
        if (wasPaused || wasQueued || notRunning) {
            val listener = req.listener
            if (listener != null) {
                req.listener = null
                executeOnMainThread {
                    listener.onError("Cancelled")
                }
            }
        }

        dbScope.launch {
            if (tempPath != null) {
                runCatching {
                    val file = File(tempPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
            dbHelper.remove(req.downloadId)
        }
    }

    /**
     * Отменяет все активные корутины загрузок и очищает базу данных в [dbScope].
     */
    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
        dbScope.launch {
            dbHelper.empty()
        }
    }

    /**
     * Фоновая очистка временных файлов и записей в БД, не обновлявшихся более [days] дней.
     */
    fun cleanup(days: Int) {
        dbScope.launch {
            val models: List<DownloadModel>? = dbHelper.getUnwantedModels(days)
            if (models != null) {
                for (model in models) {
                    val tempPath: String = getTempPath(
                        model.dirPath,
                        model.fileName
                    )
                    dbHelper.remove(model.id)
                    val file = File(tempPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }
}
