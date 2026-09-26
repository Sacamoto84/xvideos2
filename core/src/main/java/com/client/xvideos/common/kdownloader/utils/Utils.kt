package com.client.xvideos.common.kdownloader.utils

import com.client.xvideos.common.kdownloader.Constants
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.kdownloader.httpclient.DefaultHttpClient
import com.client.xvideos.common.kdownloader.httpclient.HttpClient
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/** Максимально допустимое количество цепочечных HTTP-редиректов для предотвращения зацикливания. */
private const val MAX_REDIRECTION = 10

/**
 * Формирует абсолютный путь к файлу назначения на основе каталога и имени.
 *
 * @param dirPath Каталог назначения.
 * @param fileName Имя файла.
 */
fun getPath(dirPath: String, fileName: String): String {
    return dirPath + File.separator + fileName
}

/**
 * Формирует путь к временному файлу загрузки (`.temp`).
 *
 * Загрузка всегда ведется во временный файл, чтобы исключить появление неполных/битых файлов
 * в целевой директории при внезапном обрыве соединения или аварийном завершении приложения.
 */
fun getTempPath(dirPath: String, fileName: String): String {
    return getPath(dirPath, fileName) + ".temp"
}

/**
 * Атомарное или устойчивое переименование завершенного временного файла в постоянный целевой файл.
 *
 * Включает fallback-механизм: если стандартный [File.renameTo] терпит неудачу (например,
 * перемещение между разными точками монтирования файловых систем или удержание дескриптора),
 * выполняется потоковое копирование [File.copyTo] с последующим удалением исходного файла.
 *
 * @param oldPath Исходный путь (временный файл).
 * @param newPath Целевой путь постоянного файла.
 * @throws IOException если исходный файл не существует или обе попытки (rename и copy) провалились.
 */
@Throws(IOException::class)
fun renameFileName(oldPath: String, newPath: String) {
    val oldFile = File(oldPath)
    if (!oldFile.exists()) {
        throw IOException("Source file does not exist: $oldPath")
    }
    val newFile = File(newPath)
    if (newFile.exists() && !newFile.delete()) {
        throw IOException("Deletion Failed: $newPath")
    }
    if (!oldFile.renameTo(newFile)) {
        // Fallback: кросс-файловое перемещение или временная блокировка дескриптора
        try {
            oldFile.copyTo(newFile, overwrite = true)
            oldFile.delete()
        } catch (e: Exception) {
            throw IOException("Failed to rename or copy $oldPath to $newPath", e)
        }
    }
}

/**
 * Проверяет, является ли HTTP-статус перенаправлением (301, 302, 303, 300, 307, 308).
 */
private fun isRedirection(code: Int): Boolean {
    return code == HttpURLConnection.HTTP_MOVED_PERM
            || code == HttpURLConnection.HTTP_MOVED_TEMP
            || code == HttpURLConnection.HTTP_SEE_OTHER
            || code == HttpURLConnection.HTTP_MULT_CHOICE
            || code == Constants.HTTP_TEMPORARY_REDIRECT
            || code == Constants.HTTP_PERMANENT_REDIRECT
}

/**
 * Выполняет обработку цепочки HTTP-редиректов (до [MAX_REDIRECTION] шагов) с корректным
 * разрешением относительных URI в заголовке `Location`.
 *
 * @param httpClient0 Исходный клиент с выполненным подключением.
 * @param req Запрос на загрузку (его URL мутируется на итоговый разрешенный адрес).
 * @param onNewClient Коллбэк для регистрации нового экземпляра клиента (для возможности отмены).
 * @return Финальный [HttpClient] с ответом не являющимся редиректом.
 * @throws IOException при отсутствии заголовка Location или превышении лимита редиректов.
 */
@Throws(IOException::class)
fun getRedirectedConnectionIfAny(
    httpClient0: HttpClient,
    req: DownloadRequest,
    onNewClient: (HttpClient) -> Unit = {}
): HttpClient {
    var httpClient: HttpClient = httpClient0
    var redirectTimes = 0
    var code: Int = httpClient.getResponseCode()
    var location: String? = httpClient.getResponseHeader("Location")
    while (isRedirection(code)) {
        if (location.isNullOrBlank()) {
            throw IOException("HTTP redirection code $code without Location header")
        }
        httpClient.close()
        val resolvedLocation = runCatching {
            java.net.URI(req.url).resolve(location).toString()
        }.getOrDefault(location)
        req.url = resolvedLocation
        val nextClient = DefaultHttpClient().clone()
        httpClient = nextClient
        onNewClient(nextClient)
        httpClient.connect(req)
        code = httpClient.getResponseCode()
        location = httpClient.getResponseHeader("Location")
        redirectTimes++
        if (redirectTimes >= MAX_REDIRECTION) {
            throw IOException("Too many redirects: $redirectTimes (max $MAX_REDIRECTION)")
        }
    }
    return httpClient
}

private val HEX_DIGITS = "0123456789abcdef".toCharArray()

/**
 * Вычисляет детерминированный целочисленный идентификатор загрузки на основе MD5-хэша от (url + dirPath + fileName).
 *
 * Гарантирует уникальность ID для каждой уникальной пары файл-источник.
 */
fun getUniqueId(url: String, dirPath: String, fileName: String): Int {
    val string = url + File.separator + dirPath + File.separator + fileName
    val hash: ByteArray = try {
        MessageDigest.getInstance("MD5").digest(string.toByteArray(Charsets.UTF_8))
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException("NoSuchAlgorithmException", e)
    }
    val hex = CharArray(hash.size * 2)
    for (i in hash.indices) {
        val v = hash[i].toInt() and 0xFF
        hex[i * 2] = HEX_DIGITS[v ushr 4]
        hex[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
    }
    return String(hex).hashCode()
}

/**
 * Удаляет временный файл загрузки (`.temp`), если он существует на диске.
 */
fun deleteFile(req: DownloadRequest) {
    val path = getTempPath(req.dirPath, req.fileName)
    val file = File(path)
    file.delete()
}
