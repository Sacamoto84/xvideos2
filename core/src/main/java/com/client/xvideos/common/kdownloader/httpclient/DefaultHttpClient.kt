package com.client.xvideos.common.kdownloader.httpclient

import com.client.xvideos.common.kdownloader.Constants
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.net.doh.AppDns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class DefaultHttpClient : HttpClient {
    private var response: Response? = null
    private var bodyStream: InputStream? = null

    companion object {
        private val baseOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .dns(AppDns)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    override fun clone(): HttpClient {
        return DefaultHttpClient()
    }

    @Throws(IOException::class)
    override fun connect(req: DownloadRequest) {
        val range: String = String.format(
            Locale.ENGLISH,
            "bytes=%d-", req.downloadedBytes
        )

        val builder = Request.Builder()
            .url(req.url)
            .addHeader(Constants.RANGE, range)
            .addHeader(Constants.USER_AGENT, req.userAgent)

        addHeaders(req, builder)

        val client = if (req.connectTimeOut > 0 || req.readTimeOut > 0) {
            baseOkHttpClient.newBuilder()
                .apply {
                    if (req.connectTimeOut > 0) {
                        connectTimeout(req.connectTimeOut.toLong(), TimeUnit.MILLISECONDS)
                    }
                    if (req.readTimeOut > 0) {
                        readTimeout(req.readTimeOut.toLong(), TimeUnit.MILLISECONDS)
                    }
                }
                .build()
        } else {
            baseOkHttpClient
        }

        val res = client.newCall(builder.build()).execute()
        response = res
        bodyStream = res.body.byteStream()
    }

    override fun getResponseCode(): Int = response?.code ?: 0

    override fun getInputStream(): InputStream? = bodyStream

    override fun getContentLength(): Long = response?.body?.contentLength() ?: -1L

    override fun getResponseHeader(name: String): String = response?.header(name) ?: ""

    override fun close() {
        runCatching { bodyStream?.close() }
        runCatching { response?.close() }
    }

    override fun getHeaderFields(): Map<String, List<String>> =
        response?.headers?.toMultimap() ?: emptyMap()

    override fun getErrorStream(): InputStream? = null

    private fun addHeaders(req: DownloadRequest, builder: Request.Builder) {
        val headers = req.headers ?: return
        for ((name, list) in headers) {
            for (value in list) {
                builder.addHeader(name, value)
            }
        }
    }
}
