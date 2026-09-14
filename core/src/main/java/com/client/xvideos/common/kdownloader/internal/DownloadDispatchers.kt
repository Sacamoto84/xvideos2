package com.client.xvideos.common.kdownloader.internal

import com.client.xvideos.common.kdownloader.database.DbHelper
import com.client.xvideos.common.kdownloader.database.DownloadModel
import com.client.xvideos.common.kdownloader.Status
import com.client.xvideos.common.kdownloader.utils.getTempPath
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import java.io.File

class DownloadDispatchers(private val dbHelper: DbHelper) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1) +     //main
            CoroutineExceptionHandler { _, _ ->

            })

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1) +
            CoroutineExceptionHandler { _, _ ->

            })

    fun enqueue(req: DownloadRequest): Int {
        val job = scope.launch(Dispatchers.IO.limitedParallelism(1)) {
            execute(req)
        }
        req.job = job
        return req.downloadId
    }

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

    private fun executeOnMainThread(block: () -> Unit) {
        // Колбэки слушателя (onStart/onProgress/onCompleted/...) доходят до UI,
        // поэтому выполняем их именно на главном потоке. Раньше launch шёл на
        // scope с Dispatchers.IO.limitedParallelism(1), и колбэки уходили на IO
        // вопреки имени метода.
        scope.launch(Dispatchers.Main) {
            block()
        }
    }

    fun cancel(req: DownloadRequest) {
        val wasPaused = req.status == Status.PAUSED
        val wasQueued = req.status == Status.QUEUED
        if (wasPaused) {
            val tempPath = getTempPath(req.dirPath, req.fileName)
            val file = File(tempPath)
            if (file.exists()) {
                file.delete()
            }
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
            dbHelper.remove(req.downloadId)
        }
    }

    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
        dbScope.launch {
            dbHelper.empty()
        }
    }

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
