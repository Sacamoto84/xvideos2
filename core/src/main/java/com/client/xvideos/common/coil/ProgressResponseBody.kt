package com.client.xvideos.common.coil

import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

// Класс для отслеживания прогресса загрузки
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressListener: (bytesRead: Long, contentLength: Long, done: Boolean) -> Unit
) : ResponseBody() {

    private val bufferedSource: BufferedSource by lazy { source(responseBody.source()).buffer() }

    override fun contentType() = responseBody.contentType()

    override fun contentLength() = responseBody.contentLength()

    override fun source(): BufferedSource = bufferedSource

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0L
                progressListener(
                    totalBytesRead,
                    responseBody.contentLength(),
                    bytesRead == -1L
                )
                return bytesRead
            }
        }
    }
}

