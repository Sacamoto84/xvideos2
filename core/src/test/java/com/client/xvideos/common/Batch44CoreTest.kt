package com.client.xvideos.common

import com.client.xvideos.common.kdownloader.Status
import com.client.xvideos.common.net.doh.DohAnswer
import com.client.xvideos.common.net.doh.DohDiagnosticResult
import com.client.xvideos.common.net.doh.DohProvider
import com.client.xvideos.common.net.doh.DohQuestion
import com.client.xvideos.common.net.doh.DohResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch44CoreTest {

    @Test
    fun `DohResponse and related models operate correctly`() {
        val emptyResponse = DohResponse.EMPTY
        assertTrue(emptyResponse.isSuccess)
        assertFalse(emptyResponse.hasAnswers)
        assertTrue(emptyResponse.isEmpty)
        assertFalse(emptyResponse.isNotEmpty)

        val populatedResponse = DohResponse(
            status = 0,
            question = listOf(DohQuestion(name = "example.com")),
            answer = listOf(DohAnswer(name = "example.com", type = 1, data = "93.184.216.34"))
        )
        assertTrue(populatedResponse.isSuccess)
        assertTrue(populatedResponse.hasAnswers)
        assertFalse(populatedResponse.isEmpty)
        assertTrue(populatedResponse.isNotEmpty)

        val emptyQuestion = DohQuestion.EMPTY
        assertFalse(emptyQuestion.isValid)
        assertTrue(DohQuestion(name = "example.com").isValid)

        val emptyAnswer = DohAnswer.EMPTY
        assertFalse(emptyAnswer.isValid)
        assertFalse(emptyAnswer.isAaaa)
        assertTrue(emptyAnswer.isA)

        val ipv6Answer = DohAnswer(name = "example.com", type = 28, data = "2606:2800:220:1:248:1893:25c8:1946")
        assertTrue(ipv6Answer.isValid)
        assertTrue(ipv6Answer.isAaaa)
        assertFalse(ipv6Answer.isA)

        val diag = DohDiagnosticResult("host", listOf("1.1.1.1"), 15L, "Cloudflare", true)
        assertTrue(diag.isSuccess)
        assertEquals(1, diag.count)
    }

    @Test
    fun `DohProvider helpers and fallback operate correctly`() {
        assertEquals(DohProvider.CLOUDFLARE, DohProvider.DEFAULT)
        assertTrue(DohProvider.CLOUDFLARE.isCloudflare)
        assertFalse(DohProvider.CLOUDFLARE.isGoogle)
        assertTrue(DohProvider.GOOGLE.isGoogle)
        assertTrue(DohProvider.ADGUARD.isAdGuard)
        assertTrue(DohProvider.CUSTOM.isCustom)

        assertEquals(DohProvider.GOOGLE, DohProvider.fromNameOrDefault("google"))
        assertEquals(DohProvider.CLOUDFLARE, DohProvider.fromNameOrDefault("unknown"))
        assertEquals(DohProvider.CLOUDFLARE, DohProvider.fromNameOrDefault(null))
    }

    @Test
    fun `KDownloader Status properties operate correctly`() {
        assertTrue(Status.COMPLETED.isFinished)
        assertTrue(Status.CANCELLED.isFinished)
        assertTrue(Status.FAILED.isFinished)
        assertFalse(Status.RUNNING.isFinished)
        assertFalse(Status.QUEUED.isFinished)
        assertFalse(Status.PAUSED.isFinished)

        assertTrue(Status.RUNNING.isRunning)
        assertTrue(Status.PAUSED.isPaused)
        assertTrue(Status.QUEUED.isQueued)
        assertTrue(Status.COMPLETED.isCompleted)
    }
}
