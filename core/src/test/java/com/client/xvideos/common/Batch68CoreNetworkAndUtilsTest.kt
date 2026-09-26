package com.client.xvideos.common

import com.client.xvideos.common.download.work.DownloadStatus
import com.client.xvideos.common.download.work.DownloadWorkState
import com.client.xvideos.common.gallery.GalleryTarget
import com.client.xvideos.common.io.isSafeItemName
import com.client.xvideos.common.io.isSafeRelativePath
import com.client.xvideos.common.io.normalizeRelativePathOrNull
import com.client.xvideos.common.json.isValidJsonStructure
import com.client.xvideos.common.net.doh.DohAnswer
import com.client.xvideos.common.net.doh.DohDiagnosticResult
import com.client.xvideos.common.net.doh.DohProvider
import com.client.xvideos.common.net.doh.DohQuestion
import com.client.xvideos.common.net.doh.DohResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class Batch68CoreNetworkAndUtilsTest {

    @Test
    fun `DohProvider navigation and properties`() {
        assertEquals(DohProvider.GOOGLE, DohProvider.CLOUDFLARE.next())
        assertEquals(DohProvider.CLOUDFLARE, DohProvider.CUSTOM.next())

        assertEquals(DohProvider.CUSTOM, DohProvider.CLOUDFLARE.prev())
        assertEquals(DohProvider.GOOGLE, DohProvider.ADGUARD.prev())

        assertTrue(DohProvider.CLOUDFLARE.hasBootstrapIps)
        assertFalse(DohProvider.CUSTOM.hasBootstrapIps)

        assertEquals(DohProvider.CLOUDFLARE, DohProvider.fromOrdinalOrDefault(0))
        assertEquals(DohProvider.DEFAULT, DohProvider.fromOrdinalOrDefault(99))
        assertTrue(DohProvider.allTitles.contains("Cloudflare"))
    }

    @Test
    fun `DohModels IP resolution and diagnostic summaries`() {
        val response = DohResponse(
            status = 0,
            question = listOf(DohQuestion(name = "test.com", type = 1)),
            answer = listOf(
                DohAnswer(name = "test.com", type = 1, data = "1.2.3.4"),
                DohAnswer(name = "test.com", type = 28, data = "2001:db8::1")
            )
        )

        assertEquals("1.2.3.4", response.getFirstIpv4OrNull())
        assertEquals("2001:db8::1", response.getFirstIpv6OrNull())
        assertEquals(listOf("1.2.3.4", "2001:db8::1"), response.allIpAddresses())

        val q = response.question.first()
        assertTrue(q.isA)
        assertFalse(q.isAaaa)

        val diag = DohDiagnosticResult(
            host = "test.com",
            addresses = listOf("1.2.3.4"),
            elapsedMs = 45L,
            providerTitle = "Cloudflare",
            isDoh = true
        )
        assertEquals("Cloudflare: 1 IPs in 45ms", diag.summaryText)
    }

    @Test
    fun `DownloadWorkState progress and status`() {
        assertEquals(DownloadStatus.ENQUEUED, DownloadStatus.fromNameOrDefault("ENQUEUED"))
        assertEquals(DownloadStatus.ENQUEUED, DownloadStatus.fromNameOrDefault("unknown"))
        assertTrue(DownloadStatus.ENQUEUED.isPending)

        val state = DownloadWorkState(
            workId = UUID.randomUUID(),
            tag = "video-1",
            status = DownloadStatus.RUNNING,
            progress = 40,
            bytesDownloaded = 4000L,
            totalBytes = 10000L
        )

        assertEquals(40, state.progressPercent)
        assertTrue(state.hasProgress)

        val updated = state.withProgress(80, 8000L, 10000L)
        assertEquals(80, updated.progress)
        assertEquals(8000L, updated.bytesDownloaded)
    }

    @Test
    fun `GalleryTarget mime types and extension checks`() {
        assertTrue(GalleryTarget.isVideo("video.mp4"))
        assertTrue(GalleryTarget.isVideo("movie.mkv"))
        assertFalse(GalleryTarget.isVideo("photo.jpg"))
        assertTrue(GalleryTarget.isImage("photo.png"))

        assertEquals("video/mp4", GalleryTarget.mimeTypeFor("clip.mp4"))
        assertEquals("image/jpeg", GalleryTarget.mimeTypeFor("pic.jpg"))
        assertEquals("image/webp", GalleryTarget.mimeTypeFor("pic.webp"))
        assertTrue(GalleryTarget.supportedVideoExtensions.contains("webm"))
    }

    @Test
    fun `SafePath helpers validate relative paths and item names`() {
        assertTrue(isSafeRelativePath("folder/subfolder/file.txt"))
        assertFalse(isSafeRelativePath("../escape.txt"))
        assertFalse(isSafeRelativePath("C:/windows"))

        assertEquals("a/b/c", normalizeRelativePathOrNull("/a/b/c"))
        assertNull(normalizeRelativePathOrNull("../escape"))

        assertTrue(isSafeItemName("video123"))
        assertFalse(isSafeItemName("path/with/slash"))
        assertFalse(isSafeItemName(".."))
    }

    @Test
    fun `AppJson and AppBuildInfo helpers`() {
        assertTrue(isValidJsonStructure("{\"key\": \"value\"}"))
        assertTrue(isValidJsonStructure("[1, 2, 3]"))
        assertFalse(isValidJsonStructure("invalid"))
        assertFalse(isValidJsonStructure("{unclosed"))

        AppBuildInfo.resetForTesting()
        assertFalse(AppBuildInfo.isInitialized)
        assertEquals("v?", AppBuildInfo.formatVersion())
        assertEquals("Uninitialized", AppBuildInfo.formatBuildSummary())

        AppBuildInfo.init(debug = true, versionName = "2.84")
        assertTrue(AppBuildInfo.isInitialized)
        assertTrue(AppBuildInfo.isDebug)
        assertFalse(AppBuildInfo.isRelease)
        assertEquals("v2.84", AppBuildInfo.formatVersion())
        assertEquals("v2.84 (debug)", AppBuildInfo.formatBuildSummary())
    }
}
