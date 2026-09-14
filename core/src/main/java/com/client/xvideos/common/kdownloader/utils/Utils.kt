package com.client.xvideos.common.kdownloader.utils

import com.client.xvideos.common.kdownloader.Constants
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.kdownloader.httpclient.DefaultHttpClient
import com.client.xvideos.common.kdownloader.httpclient.HttpClient
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.experimental.and

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

fun getUniqueId(url: String, dirPath: String, fileName: String): Int {
    val string = url + File.separator + dirPath + File.separator + fileName
    val hash: ByteArray = try {
        MessageDigest.getInstance("MD5").digest(string.toByteArray(charset("UTF-8")))
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException("NoSuchAlgorithmException", e)
    } catch (e: UnsupportedEncodingException) {
        throw RuntimeException("UnsupportedEncodingException", e)
    }
    val hex = StringBuilder(hash.size * 2)
    for (b in hash) {
        val v = b.toInt() and 0xFF
        if (v < 0x10) hex.append("0")
        hex.append(Integer.toHexString(v))
    }
    return hex.toString().hashCode()
}

fun deleteFile(req: DownloadRequest) {
    val path = getTempPath(req.dirPath, req.fileName)
    val file = File(path)
    file.delete()
}
