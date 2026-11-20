package com.client.xvideos.common.kdownloader.internal

import com.client.xvideos.common.kdownloader.Constants
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
                    request.takeListener()?.onCompleted()
                }
            },
            onError = {
                executeOnMainThread {
                    request.takeListener()?.onError(it)
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
        val tempPath = if (wasPaused) getTempPath(req.dirPath, req.fileName) else null
        if (wasPaused) {
            req.reset()
        }

        req.status = Status.CANCELLED
        val listener = req.takeListener()
        val job = req.job
        job?.cancel()

        dbScope.launch {
            try {
                job?.join()
                if (tempPath != null) {
                    File(tempPath).delete()
                }
                dbHelper.remove(req.downloadId)
            } finally {
                // Архивирование может продолжиться сразу после колбэка отмены.
                if (listener != null) executeOnMainThread { listener.onError(Constants.CANCELLED) }
            }
        }
    }

    /**
     * Отменяет все активные корутины загрузок этого экземпляра.
     *
     * Базу целиком не очищает: файл базы один на все экземпляры загрузчика, и
     * очистка стирала записи чужих загрузок — «удалить всё» в одном разделе
     * лишало докачки загрузки другого. Записи своих запросов удаляет [cancel],
     * который очередь зовёт для каждого из них.
     */
    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
    }

    /**
     * Фоновая очистка временных файлов и записей в БД, не обновлявшихся более [days] дней.
     */
    fun cleanup(days: Int) {
        dbScope.launch { removeStaleDownloads(dbHelper, days) }
    }
}

/**
 * Убирает записи загрузок, не обновлявшиеся более [days] дней, и их временные файлы.
 *
 * Временный файл удаляется, только если и сам он столько же не менялся. Запись
 * прежней попытки указывает на тот же `.temp`, что и новая: id загрузки
 * считается от адреса, а адрес у повтора другой. Без этой проверки уборка
 * стирала бы файл, в который прямо сейчас пишет идущая загрузка.
 *
 * @param nowMs Текущее время; подменяется в тестах.
 */
internal suspend fun removeStaleDownloads(
    dbHelper: DbHelper,
    days: Int,
    nowMs: Long = System.currentTimeMillis(),
) {
    val models: List<DownloadModel> = dbHelper.getUnwantedModels(days) ?: return
    val staleBefore = nowMs - days * MILLIS_PER_DAY
    for (model in models) {
        dbHelper.remove(model.id)
        val file = File(getTempPath(model.dirPath, model.fileName))
        if (file.exists() && file.lastModified() <= staleBefore) {
            file.delete()
        }
    }
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
