package com.client.xvideos.common.kdownloader

import android.content.Context
import com.client.xvideos.common.kdownloader.database.AppDbHelper
import com.client.xvideos.common.kdownloader.database.DbHelper
import com.client.xvideos.common.kdownloader.database.NoOpsDbHelper
import com.client.xvideos.common.kdownloader.internal.DownloadDispatchers
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.kdownloader.internal.DownloadRequestQueue

class KDownloader private constructor(dbHelper: DbHelper, private val config: DownloaderConfig) {

    companion object {
        fun create(
            context: Context,
            config: DownloaderConfig = DownloaderConfig(true)
        ): KDownloader {
            return if (config.databaseEnabled) {
                KDownloader(AppDbHelper(context), config)
            } else {
                KDownloader(NoOpsDbHelper(), config)
            }
        }

        fun createForTesting(
            dbHelper: DbHelper = NoOpsDbHelper(),
            config: DownloaderConfig = DownloaderConfig(false)
        ): KDownloader = KDownloader(dbHelper, config)
    }

    private val downloader = DownloadDispatchers(dbHelper)
    private val reqQueue = DownloadRequestQueue(downloader)

    fun newRequestBuilder(url: String, dirPath: String, fileName: String): DownloadRequest.Builder {
        return DownloadRequest.Builder(url, dirPath, fileName)
            .readTimeout(config.readTimeOut)
            .connectTimeout(config.connectTimeOut)
    }

    fun enqueue(req: DownloadRequest, listener: DownloadRequest.Listener): Int {
        val wrappedListener = object : DownloadRequest.Listener {
            override fun onStart() = listener.onStart()
            override fun onProgress(value: Int) = listener.onProgress(value)
            override fun onPause() = listener.onPause()
            override fun onError(error: String) {
                try {
                    listener.onError(error)
                } finally {
                    reqQueue.remove(req.downloadId)
                }
            }
            override fun onCompleted() {
                try {
                    listener.onCompleted()
                } finally {
                    reqQueue.remove(req.downloadId)
                }
            }
        }
        req.listener = wrappedListener
        return reqQueue.enqueue(req)
    }

    fun remove(id: Int) {
        reqQueue.remove(id)
    }

    inline fun enqueue(
        req: DownloadRequest,
        crossinline onStart: () -> Unit = {},
        crossinline onProgress: (value: Int) -> Unit = { _ -> },
        crossinline onPause: () -> Unit = {},
        crossinline onError: (error: String) -> Unit = { _ -> },
        crossinline onCompleted: () -> Unit = {}
    ) = enqueue(req, object : DownloadRequest.Listener {
        override fun onStart() = onStart()
        override fun onProgress(value: Int) = onProgress(value)
        override fun onPause() = onPause()
        override fun onError(error: String) = onError(error)
        override fun onCompleted() = onCompleted()
    })

    fun status(id: Int): Status {
        return reqQueue.status(id)
    }

    fun cancel(id: Int) {
        reqQueue.cancel(id)
    }

    fun cancel(tag: String) {
        reqQueue.cancel(tag)
    }

    fun cancelAll() {
        reqQueue.cancelAll()
    }

    fun pause(id: Int) {
        reqQueue.pause(id)
    }

    fun resume(id: Int) {
        reqQueue.resume(id)
    }

    fun cleanUp(days: Int) {
        downloader.cleanup(days)

    }

    /**
     * Получить статусы всех запросов с указанным тегом
     */
    fun getStatusesByTag(tag: String): List<Pair<Int, Status>>{
        return reqQueue.getStatusesByTag(tag)
    }

}
