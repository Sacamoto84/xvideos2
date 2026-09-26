package com.client.xvideos.r

import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.block.useCase.getBlockFile
import com.client.xvideos.r.common.block.useCase.isItemBlockedOnDisk
import com.client.xvideos.r.common.downloader.Downloader
import com.client.xvideos.r.common.downloader.RedDownloadEnqueueReport
import com.client.xvideos.r.common.downloader.RedDownloadRecoveryReport
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.URL1
import com.client.xvideos.r.model.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

class Batch78RDownloaderAndBlockTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_r_batch78").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    @Test
    fun testBlockItemHelpers() {
        assertNull(getBlockFile("", "123"))
        assertNull(getBlockFile("user", ""))
        assertNull(getBlockFile("../hacker", "123"))
        assertNull(getBlockFile("user", "../malicious"))

        val safeFile = getBlockFile("goodUser", "item42")
        assertNotNull(safeFile)
        assertTrue(safeFile!!.path.replace('\\', '/').endsWith("goodUser/item42.block"))

        assertFalse(isItemBlockedOnDisk("../hacker", "123"))
    }

    @Test
    fun testBlockRedHelpers() {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val blockRed = BlockRed(testScope)

        blockRed.blockItem = GifsInfo(id = "gif1", userName = "author1")
        blockRed.blockVisibleDialog = true
        blockRed.clearDialogSelection()
        assertNull(blockRed.blockItem)
        assertFalse(blockRed.blockVisibleDialog)

        val items = listOf(
            GifsInfo(id = "gif1", userName = "author1"),
            GifsInfo(id = "gif2", userName = "author2")
        )
        assertFalse(blockRed.isAnyBlocked(items))
        assertEquals(2, blockRed.filterUnblocked(items).size)
    }

    @Test
    fun testDownloaderAndReportHelpers() {
        val cleanReport = RedDownloadEnqueueReport(queuedVideo = 2, queuedPreview = 2)
        assertEquals(4, cleanReport.totalQueued)
        assertEquals(0, cleanReport.totalSkipped)
        assertTrue(cleanReport.hasQueued)
        assertTrue(cleanReport.isClean)

        val skippedReport = RedDownloadEnqueueReport(skippedNoVideoUrl = 1, skippedNoPreviewUrl = 1)
        assertEquals(0, skippedReport.totalQueued)
        assertEquals(2, skippedReport.totalSkipped)
        assertFalse(skippedReport.hasQueued)
        assertFalse(skippedReport.isClean)

        val recoveryClean = RedDownloadRecoveryReport(incompleteItems = 0, invalidInfoFiles = 0)
        assertTrue(recoveryClean.isClean)
        assertFalse(recoveryClean.hasIncomplete)
        assertFalse(recoveryClean.hasErrors)

        val recoveryIncomplete = RedDownloadRecoveryReport(incompleteItems = 2, invalidInfoFiles = 1, queuedVideo = 2)
        assertFalse(recoveryIncomplete.isClean)
        assertTrue(recoveryIncomplete.hasIncomplete)
        assertTrue(recoveryIncomplete.hasErrors)
        assertEquals(2, recoveryIncomplete.totalQueued)

        val testDownloader = KDownloader.createForTesting()
        val downloader = Downloader(testDownloader)
        assertTrue(downloader.isIdle())
        assertFalse(downloader.isDownloading())
        assertFalse(downloader.hasDownloadError())

        downloader.percent.value = 0.5f
        assertTrue(downloader.isDownloading())
        assertFalse(downloader.isIdle())

        downloader.percent.value = -3f
        assertTrue(downloader.hasDownloadError())
    }

    @Test
    fun testUsersRedBatchAndSearch() {
        UsersRed.clear()
        val user1 = UserInfo(username = "jane_doe", name = "Jane Doe")
        val user2 = UserInfo(username = "john_smith", name = "John Smith")
        UsersRed.addUsers(listOf(user1, user2))

        assertEquals(2, UsersRed.count)
        assertEquals(setOf("jane_doe", "john_smith"), UsersRed.getAllUsernames())

        val matched = UsersRed.findUsersMatching("jane")
        assertEquals(1, matched.size)
        assertEquals("jane_doe", matched[0].username)

        assertTrue(UsersRed.findUsersMatching("").isEmpty())
        UsersRed.clear()
    }

    @Test
    fun testURL1Helpers() {
        val urlSound = URL1(hd = "https://media.redgifs.com/vid.mp4", sd = "https://media.redgifs.com/sd.mp4")
        assertTrue(urlSound.hasSound)
        assertFalse(urlSound.isSilentOnly)

        val urlSilent = URL1(silent = "https://media.redgifs.com/silent.mp4", sd = "https://media.redgifs.com/sd.mp4")
        assertFalse(urlSilent.hasSound)
        assertTrue(urlSilent.isSilentOnly)

        val modified = urlSilent.withHd("https://media.redgifs.com/hd.mp4")
            .withPoster("https://media.redgifs.com/poster.jpg")
            .withThumbnail("https://media.redgifs.com/thumb.jpg")
            .withSd("https://media.redgifs.com/custom_sd.mp4")

        assertTrue(modified.hasSound)
        assertEquals("https://media.redgifs.com/hd.mp4", modified.hd)
        assertEquals("https://media.redgifs.com/poster.jpg", modified.poster)
        assertEquals("https://media.redgifs.com/thumb.jpg", modified.thumbnail)
        assertEquals("https://media.redgifs.com/custom_sd.mp4", modified.sd)
    }

    @Test
    fun testGifsInfoOrientationAndTags() {
        val portrait = GifsInfo(
            id = "g1",
            width = 720,
            height = 1280,
            tags = listOf("Bikini", "Beach"),
            niches = listOf("fitness", "glamour")
        )
        assertTrue(portrait.isPortrait)
        assertFalse(portrait.isLandscape)
        assertFalse(portrait.isSquare)
        assertTrue(portrait.hasTag("bikini"))
        assertTrue(portrait.hasTag("BEACH"))
        assertFalse(portrait.hasTag("unknown"))
        assertTrue(portrait.hasNiche("fitness"))
        assertTrue(portrait.hasNiche("GLAMOUR"))
        assertFalse(portrait.hasNiche("gaming"))

        val landscape = GifsInfo(id = "g2", width = 1920, height = 1080)
        assertTrue(landscape.isLandscape)
        assertFalse(landscape.isPortrait)
        assertFalse(landscape.isSquare)

        val square = GifsInfo(id = "g3", width = 600, height = 600)
        assertTrue(square.isSquare)
        assertFalse(square.isPortrait)
        assertFalse(square.isLandscape)
    }

    @Test
    fun testOrderAndMediaTypeCollections() {
        assertTrue(Order.allNames.contains("LATEST"))
        assertTrue(Order.allValues.contains("trending"))

        assertTrue(MediaType.allNames.contains("IMAGE"))
        assertTrue(MediaType.allValues.contains("all"))
        assertTrue(MediaType.allValues.contains("g"))
    }

    @Test
    fun testUserInfoAndNichesInfoHelpers() {
        val user = UserInfo(username = "star", views = 1500L, gifs = 0L)
        assertTrue(user.hasGifsOrViews)

        val withAvatar = user.withAvatar("https://pic.jpg")
            .withDescription("My bio")
            .withVerified(true)
        assertEquals("https://pic.jpg", withAvatar.profileImageUrl)
        assertEquals("My bio", withAvatar.description)
        assertTrue(withAvatar.verified)

        val niche = NichesInfo(id = "summer", name = "Summer Vibes", subscribers = 2_500_000L, gifs = 120L)
        assertTrue(niche.hasStats)
        assertEquals("2.5M", niche.formatSubscribers())

        val smallNiche = NichesInfo(id = "small", subscribers = 4500L)
        assertEquals("4.5k", smallNiche.formatSubscribers())

        val zeroNiche = NichesInfo(id = "zero", subscribers = 0L)
        assertEquals("0", zeroNiche.formatSubscribers())

        val withImages = niche.withCover("https://cover.jpg").withThumbnail("https://thumb.jpg")
        assertEquals("https://cover.jpg", withImages.cover)
        assertEquals("https://thumb.jpg", withImages.thumbnail)
    }
}
