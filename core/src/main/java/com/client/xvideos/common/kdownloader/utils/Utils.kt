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

private const val MAX_REDIRECTION = 10

fun getPath(dirPath: String, fileName: String): String {
    return dirPath + File.separator + fileName
}

fun getTempPath(dirPath: String, fileName: String): String {
    return getPath(dirPath, fileName) + ".temp"
}

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
        // Fallback: cross-filesystem move or temporary file handle lock
        try {
            oldFile.copyTo(newFile, overwrite = true)
            oldFile.delete()
        } catch (e: Exception) {
            throw IOException("Failed to rename or copy $oldPath to $newPath", e)
        }
    }
}

private fun isRedirection(code: Int): Boolean {
    return code == HttpURLConnection.HTTP_MOVED_PERM
            || code == HttpURLConnection.HTTP_MOVED_TEMP
            || code == HttpURLConnection.HTTP_SEE_OTHER
            || code == HttpURLConnection.HTTP_MULT_CHOICE
            || code == Constants.HTTP_TEMPORARY_REDIRECT
            || code == Constants.HTTP_PERMANENT_REDIRECT
}

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

fun deleteFile(req: DownloadRequest) {
    val path = getTempPath(req.dirPath, req.fileName)
    val file = File(path)
    file.delete()
}
