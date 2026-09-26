package com.client.xvideos.common.kdownloader.internal

import com.client.xvideos.common.kdownloader.Constants
import com.client.xvideos.common.kdownloader.Status
import com.client.xvideos.common.kdownloader.utils.getUniqueId
import kotlinx.coroutines.Job

/**
 * Внутреннее представление отдельного запроса на скачивание файла.
 *
 * Инкапсулирует параметры источника (URL, заголовки, User-Agent), целевого файла (директория, имя),
 * таймаутов, текущего состояния выполнения и ссылки на активную корутину [job].
 */
class DownloadRequest private constructor(
    internal var url: String,
    internal val tag: String?,
    internal var listener: Listener?,
    internal val headers: HashMap<String, List<String>>?,
    internal val dirPath: String,
    internal val downloadId: Int,
    internal val fileName: String,
    internal var status: Status = Status.UNKNOWN,
    internal var readTimeOut: Int = 0,
    internal var connectTimeOut: Int = 0,
    internal var userAgent: String = Constants.DEFAULT_USER_AGENT
) {

    /** Полный размер файла в байтах. */
    var totalBytes: Long = 0

    /** Количество уже скачанных байт. */
    var downloadedBytes: Long = 0

    /** Активная корутинная задача выполнения загрузки. */
    internal var job: Job? = null

    /**
     * Построитель (Builder) для создания [DownloadRequest] с пользовательскими параметрами.
     *
     * @param url URL файла для скачивания.
     * @param dirPath Локальная директория сохранения.
     * @param fileName Имя сохраняемого файла.
     */
    data class Builder(
        private val url: String, private val dirPath: String, private val fileName: String
    ) {

        private var tag: String? = null
        private var listener: Listener? = null
        private var headers: HashMap<String, List<String>>? = null
        private var readTimeOut: Int = Constants.DEFAULT_READ_TIMEOUT_IN_MILLS
        private var connectTimeOut: Int = Constants.DEFAULT_CONNECT_TIMEOUT_IN_MILLS
        private var userAgent: String = Constants.DEFAULT_USER_AGENT

        /** Опциональный тег для группового управления (отмена/получение статусов группы загрузок). */
        fun tag(tag: String) = apply {
            this.tag = tag
        }

        /** Пользовательские HTTP-заголовки. */
        fun headers(headers: HashMap<String, List<String>>) = apply {
            this.headers = headers
        }

        /** Таймаут чтения сокета в миллисекундах. */
        fun readTimeout(timeout: Int) = apply {
            this.readTimeOut = timeout
        }

        /** Таймаут подключения в миллисекундах. */
        fun connectTimeout(timeout: Int) = apply {
            this.connectTimeOut = timeout
        }

        /** Пользовательский заголовок User-Agent. */
        fun userAgent(userAgent: String) = apply {
            this@Builder.userAgent = userAgent
        }

        /** Создает сконфигурированный экземпляр [DownloadRequest] с вычислением детерминированного ID. */
        fun build(): DownloadRequest {
            return DownloadRequest(
                url = url,
                tag = tag,
                listener = listener,
                headers = headers,
                dirPath = dirPath,
                downloadId = getUniqueId(url, dirPath, fileName),
                fileName = fileName,
                readTimeOut = readTimeOut,
                connectTimeOut = connectTimeOut,
                userAgent = userAgent
            )
        }
    }

    /**
     * Слушатель событий жизненного цикла отдельной загрузки.
     * Вызовы гарантированно доставляются в главный поток UI через callbackScope.
     */
    interface Listener {
        /** Вызывается перед началом сетевого соединения. */
        fun onStart()

        /**
         * Вызывается при обновлении прогресса.
         * @param value Процент выполнения [0..100].
         */
        fun onProgress(value: Int)

        /** Вызывается при успешной приостановке загрузки. */
        fun onPause()

        /** Вызывается при успешном завершении скачивания и валидации файла. */
        fun onCompleted()

        /** Вызывается при фатальной ошибке или отмене задачи. */
        fun onError(error: String)
    }

    /**
     * Сбрасывает счетчики байт и статус задачи в начальное состояние.
     */
    fun reset(){
        downloadedBytes = 0
        totalBytes = 0
        status = Status.UNKNOWN
    }

}
