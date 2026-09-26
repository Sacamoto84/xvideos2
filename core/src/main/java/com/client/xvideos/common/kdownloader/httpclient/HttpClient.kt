package com.client.xvideos.common.kdownloader.httpclient

import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import java.io.IOException
import java.io.InputStream

/**
 * Абстракция HTTP-клиента для сетевых операций скачивания в [com.client.xvideos.common.kdownloader.KDownloader].
 *
 * Инкапсулирует протокол HTTP, управление потоками данных, заголовками Range и таймаутами.
 */
interface HttpClient : Cloneable {

    /** Возвращает размер содержимого (Content-Length) в байтах либо -1L, если размер неизвестен. */
    fun getContentLength(): Long

    /** Возвращает карту всех заголовков ответа сервера. */
    fun getHeaderFields(): Map<String, List<String>>

    /**
     * HTTP-код ответа сервера (например, 200, 206, 404).
     * @throws IOException при сетевом сбое.
     */
    @Throws(IOException::class)
    fun getResponseCode(): Int

    /**
     * Возвращает входящий поток тела ответа для чтения байтов.
     * @throws IOException при ошибке открытия потока.
     */
    @Throws(IOException::class)
    fun getInputStream(): InputStream?

    /** Возвращает поток ошибки (для анализа неуспешных кодов ответа). */
    fun getErrorStream(): InputStream?

    /** Создает независимый клон конфигурации клиента (например, для следования по редиректам). */
    public override fun clone(): HttpClient

    /**
     * Выполняет синхронное подключение к серверу по параметрам запроса [req].
     *
     * @param req Параметры запроса (URL, смещение байт, заголовки, таймауты).
     * @throws IOException при сбое подключения или отмене.
     */
    @Throws(IOException::class)
    fun connect(req: DownloadRequest)

    /** Возвращает значение конкретного заголовка ответа по его имени или пустую строку. */
    fun getResponseHeader(name: String): String

    /** Освобождает сетевые ресурсы, сокеты и закрывает потоки ввода/вывода. */
    fun close()

}
