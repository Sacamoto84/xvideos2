package com.client.xvideos.common.kdownloader.internal

import com.client.xvideos.common.kdownloader.Status
import java.util.concurrent.ConcurrentHashMap

/**
 * Потокобезопасная очередь запросов на скачивание.
 *
 * Управляет активным реестром задач в памяти ([idRequestMap]), фильтрацией по тегам,
 * дедупликацией повторных запросов, а также операциями паузы, возобновления и групповой отмены.
 *
 * @param downloader Диспетчер корутин для фактического выполнения задач.
 */
class DownloadRequestQueue(private val downloader: DownloadDispatchers) {

    /** Реестр активных запросов в памяти с потокобезопасным доступом. */
    private val idRequestMap = ConcurrentHashMap<Int, DownloadRequest>()

    /**
     * Возвращает все активные запросы с указанным тегом.
     *
     * @param tag Пользовательский тег группы загрузок.
     */
    fun getRequestsByTag(tag: String): List<DownloadRequest> {
        return idRequestMap.values.filter { it.tag == tag }
    }

    /**
     * Возвращает пары (ID загрузки, статус) для всех запросов с указанным тегом.
     */
    fun getStatusesByTag(tag: String): List<Pair<Int, Status>> {
        return idRequestMap.values
            .filter { it.tag == tag }
            .map { it.downloadId to it.status }
    }

    /**
     * Возвращает ассоциативный массив ID -> Status для всех запросов с указанным тегом.
     */
    fun getStatusMapByTag(tag: String): Map<Int, Status> {
        return idRequestMap.values
            .filter { it.tag == tag }
            .associate { it.downloadId to it.status }
    }

    /**
     * Возвращает снимок всех текущих запросов очереди.
     */
    fun getAllRequests(): List<DownloadRequest> {
        return idRequestMap.values.toList()
    }

    /**
     * Возвращает мапу всех текущих статусов (ID -> Status).
     */
    fun getAllStatuses(): Map<Int, Status> {
        return idRequestMap.mapValues { it.value.status }
    }

    /**
     * Возвращает агрегированное количество задач по каждому статусу для указанного тега.
     */
    fun getStatusCountsByTag(tag: String): Map<Status, Int> {
        return idRequestMap.values
            .filter { it.tag == tag }
            .groupBy { it.status }
            .mapValues { it.value.size }
    }

    /**
     * Помещает запрос в очередь с проверкой на дедупликацию.
     * Если задача с таким ID уже находится в очереди или выполняется, повторный запуск игнорируется.
     *
     * @param request Запрос на загрузку.
     * @return Идентификатор загрузки.
     */
    fun enqueue(request: DownloadRequest): Int {
        val existing = idRequestMap[request.downloadId]
        if (existing != null && (existing.status == Status.QUEUED || existing.status == Status.RUNNING)) {
            return existing.downloadId
        }
        request.status = Status.QUEUED
        idRequestMap[request.downloadId] = request
        return downloader.enqueue(request)
    }

    /**
     * Возвращает текущий статус задачи по её ID (или [Status.UNKNOWN], если задачи нет в памяти).
     */
    fun status(id: Int): Status {
        val req = idRequestMap[id] ?: return Status.UNKNOWN
        return req.status
    }

    /**
     * Отменяет задачу и удаляет её из реестра очереди.
     */
    fun cancel(id: Int) {
        val req = idRequestMap[id]
        if (req != null && req.status != Status.CANCELLED) {
            downloader.cancel(req)
        }
        idRequestMap.remove(id)
    }

    /**
     * Удаляет задачу из реестра очереди в памяти (без отмены файла, если она уже завершена).
     */
    fun remove(id: Int) {
        idRequestMap.remove(id)
    }

    /**
     * Отменяет все задачи, помеченные указанным тегом.
     */
    fun cancel(tag: String) {
        val list = idRequestMap.values.filter {
            it.tag == tag
        }

        for (req in list) {
            cancel(req.downloadId)
        }
    }

    /**
     * Отменяет все задачи в очереди и очищает диспетчер.
     */
    fun cancelAll() {
        val list = idRequestMap.values.toList()
        for (req in list) {
            cancel(req.downloadId)
        }
        downloader.cancelAll()
    }

    /**
     * Переводит выполняющуюся задачу в статус [Status.PAUSED].
     */
    fun pause(id: Int) {
        val req = idRequestMap[id] ?: return
        if (req.status != Status.RUNNING && req.status != Status.QUEUED) {
            return
        }
        req.status = Status.PAUSED
    }

    /**
     * Возобновляет приостановленную задачу [Status.PAUSED], отправляя её обратно в диспетчер.
     */
    fun resume(id: Int) {
        val req = idRequestMap[id] ?: return
        if (req.status != Status.PAUSED) {
            return
        }
        req.status = Status.QUEUED
        downloader.enqueue(req)
    }
}
