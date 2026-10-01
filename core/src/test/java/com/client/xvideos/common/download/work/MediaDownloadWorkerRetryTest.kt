package com.client.xvideos.common.download.work

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class MediaDownloadWorkerRetryTest {

    @Test
    fun `сбой сети повторяется`() {
        assertTrue(MediaDownloadWorker.isRetryable(IOException("Download interrupted")))
        assertTrue(MediaDownloadWorker.isRetryable(SocketTimeoutException("timeout")))
    }

    @Test
    fun `5xx, 408 и 429 повторяются`() {
        assertTrue(MediaDownloadWorker.isRetryable(MediaDownloadWorker.HttpStatusException(503, "HTTP 503")))
        assertTrue(MediaDownloadWorker.isRetryable(MediaDownloadWorker.HttpStatusException(408, "HTTP 408")))
        assertTrue(MediaDownloadWorker.isRetryable(MediaDownloadWorker.HttpStatusException(429, "HTTP 429")))
    }

    @Test
    fun `остальные 4xx и не сетевые ошибки не повторяются`() {
        assertFalse(MediaDownloadWorker.isRetryable(MediaDownloadWorker.HttpStatusException(404, "HTTP 404")))
        assertFalse(MediaDownloadWorker.isRetryable(MediaDownloadWorker.HttpStatusException(403, "HTTP 403")))
        assertFalse(MediaDownloadWorker.isRetryable(IllegalStateException("bug")))
    }
}
