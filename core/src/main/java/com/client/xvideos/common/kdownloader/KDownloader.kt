package com.client.xvideos.common.kdownloader

import android.content.Context
import com.client.xvideos.common.kdownloader.database.AppDbHelper
import com.client.xvideos.common.kdownloader.database.DbHelper
import com.client.xvideos.common.kdownloader.database.NoOpsDbHelper
import com.client.xvideos.common.kdownloader.internal.DownloadDispatchers
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.kdownloader.internal.DownloadRequestQueue

/**
 * Главная точка входа для управления загрузками файлов в приложении.
 *
 * Предоставляет высокоуровневый фасад над очередью задач [DownloadRequestQueue],
 * корутинным диспетчером [DownloadDispatchers] и локальной базой данных [DbHelper].
 *
 * Поддерживает:
 * - Потокобезопасную постановку в очередь и отслеживание прогресса.
 * - HTTP-докачку файлов (Range, ETag) с защитой от повреждений через временные `.temp` файлы.
 * - Приостановку, возобновление и групповую отмену по тегам.
 * - Автоматическую очистку очереди по завершении или ошибке задачи.
 */
class KDownloader private constructor(dbHelper: DbHelper, private val config: DownloaderConfig) {

    companion object {
        /**
         * Фабричный метод инициализации [KDownloader] для рабочего окружения Android.
         *
         * @param context Контекст приложения для открытия SQLite БД.
         * @param config Конфигурация таймаутов и флага активности БД.
         */
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

        /**
         * Фабричный метод для модульного тестирования (позволяет внедрить mock/fake [DbHelper]).
         */
        fun createForTesting(
            dbHelper: DbHelper = NoOpsDbHelper(),
            config: DownloaderConfig = DownloaderConfig(false)
        ): KDownloader = KDownloader(dbHelper, config)
    }

    private val downloader = DownloadDispatchers(dbHelper)
    private val reqQueue = DownloadRequestQueue(downloader)

    /**
     * Создает новый [DownloadRequest.Builder], преднастроенный таймаутами из текущего [config].
     *
     * @param url Сетевой адрес загружаемого ресурса.
     * @param dirPath Каталог сохранения файла.
     * @param fileName Имя сохраняемого файла.
     */
    fun newRequestBuilder(url: String, dirPath: String, fileName: String): DownloadRequest.Builder {
        return DownloadRequest.Builder(url, dirPath, fileName)
            .readTimeout(config.readTimeOut)
            .connectTimeout(config.connectTimeOut)
    }

    /**
     * Помещает запрос в очередь выполнения со слушателем [listener].
     * Автоматически удаляет задачу из очереди при ошибке или успешном завершении.
     *
     * @param req Запрос на загрузку.
     * @param listener Интерфейс обратного вызова событий жизненного цикла.
     * @return Целочисленный идентификатор загрузки.
     */
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

    /**
     * Удаляет запрос с указанным ID из оперативной очереди в памяти.
     */
    fun remove(id: Int) {
        reqQueue.remove(id)
    }

    /**
     * Kotlin-friendly DSL-перегрузка [enqueue] с лямбда-функциями обратного вызова.
     */
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

    /**
     * Возвращает текущий статус загрузки по её идентификатору.
     */
    fun status(id: Int): Status {
        return reqQueue.status(id)
    }

    /**
     * Отменяет задачу по ID, останавливает поток данных и удаляет временные файлы.
     */
    fun cancel(id: Int) {
        reqQueue.cancel(id)
    }

    /**
     * Отменяет все задачи с указанным тегом.
     */
    fun cancel(tag: String) {
        reqQueue.cancel(tag)
    }

    /**
     * Отменяет абсолютно все активные и ожидающие задачи.
     */
    fun cancelAll() {
        reqQueue.cancelAll()
    }

    /**
     * Приостанавливает выполнение задачи по ID (для последующего возобновления).
     */
    fun pause(id: Int) {
        reqQueue.pause(id)
    }

    /**
     * Возобновляет приостановленную задачу.
     */
    fun resume(id: Int) {
        reqQueue.resume(id)
    }

    /**
     * Очищает временные файлы и записи БД старше [days] дней.
     */
    fun cleanUp(days: Int) {
        downloader.cleanup(days)
    }

    /**
     * Получить статусы всех запросов с указанным тегом.
     */
    fun getStatusesByTag(tag: String): List<Pair<Int, Status>>{
        return reqQueue.getStatusesByTag(tag)
    }

}
