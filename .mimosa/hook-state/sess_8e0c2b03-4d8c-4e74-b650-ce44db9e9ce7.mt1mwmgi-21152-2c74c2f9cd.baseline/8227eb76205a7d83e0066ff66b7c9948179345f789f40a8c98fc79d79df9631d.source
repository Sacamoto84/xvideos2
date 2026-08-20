package com.client.xvideos.common.kdownloader.internal

import com.client.xvideos.common.kdownloader.Status

class DownloadRequestQueue(private val downloader: DownloadDispatchers) {

    private val idRequestMap: HashMap<Int, DownloadRequest> = hashMapOf()

    /**
    * Получить все запросы с указанным тегом
    */
    fun getRequestsByTag(tag: String): List<DownloadRequest> {
        return idRequestMap.values.filter { it.tag == tag }
    }

    /**
     * Получить статусы всех запросов с указанным тегом
     */
    fun getStatusesByTag(tag: String): List<Pair<Int, Status>> {
        return idRequestMap.values
            .filter { it.tag == tag }
            .map { it.downloadId to it.status }
    }

    /**
     * Получить статусы всех запросов с указанным тегом в виде Map
     */
    fun getStatusMapByTag(tag: String): Map<Int, Status> {
        return idRequestMap.values
            .filter { it.tag == tag }
            .associate { it.downloadId to it.status }
    }

    /**
     * Получить все запросы (без фильтрации)
     */
    fun getAllRequests(): List<DownloadRequest> {
        return idRequestMap.values.toList()
    }

    /**
     * Получить все статусы (без фильтрации)
     */
    fun getAllStatuses(): Map<Int, Status> {
        return idRequestMap.mapValues { it.value.status }
    }

    /**
     * Получить количество запросов по статусам для указанного тега
     */
    fun getStatusCountsByTag(tag: String): Map<Status, Int> {
        return idRequestMap.values
            .filter { it.tag == tag }
            .groupBy { it.status }
            .mapValues { it.value.size }
    }

    fun enqueue(request: DownloadRequest): Int {
        idRequestMap[request.downloadId] = request
        return downloader.enqueue(request)
    }

    fun status(id: Int): Status {
        val req = idRequestMap[id] ?: return Status.UNKNOWN
        return req.status
    }

    fun cancel(id: Int) {
        val req = idRequestMap[id]
        if (req != null && req.status != Status.CANCELLED) {
            downloader.cancel(req)
        }
        idRequestMap.remove(id)
    }



    fun cancel(tag: String) {
        val list = idRequestMap.values.filter {
            it.tag == tag
        }

        for (req in list) {
            cancel(req.downloadId)
        }
    }

    fun cancelAll() {
        idRequestMap.clear()
        downloader.cancelAll()
    }

    fun pause(id: Int) {
        val req = idRequestMap[id] ?: return
        req.status = Status.PAUSED
    }

    fun resume(id: Int) {
        val req = idRequestMap[id] ?: return
        req.status = Status.QUEUED
        downloader.enqueue(req)
    }
}
