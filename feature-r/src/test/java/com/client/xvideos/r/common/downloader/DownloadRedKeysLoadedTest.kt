package com.client.xvideos.r.common.downloader

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.kdownloader.KDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Набор скачанных роликов отличает «каталог ещё не прочитан» от «прочитан и
 * пуст» (проход 208, C1). Полноэкранный R запоминает адрес ролика один раз:
 * увидев пустой набор до конца обхода каталога, он играл скачанный ролик из сети.
 */
class DownloadRedKeysLoadedTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var context: ContextWrapper
    private val scope = CoroutineScope(SupervisorJob())

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_r_keys")
        context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun downloadRed(scope: CoroutineScope) =
        DownloadRed(Downloader(KDownloader.createForTesting()), scope, context)

    private fun awaitKeys(downloadRed: DownloadRed): Set<String> = runBlocking {
        withTimeout(10_000) { downloadRed.downloadedVideoKeys.filterNotNull().first() }
    }

    @Test
    fun `до первого обхода каталога набор не прочитан`() {
        // Скоуп отменён: обход из init не запускается, и видно начальное значение.
        val stopped = CoroutineScope(Job().apply { cancel() })

        assertNull(downloadRed(stopped).downloadedVideoKeys.value)
    }

    @Test
    fun `без загрузок набор прочитан и пуст`() {
        assertTrue(awaitKeys(downloadRed(scope)).isEmpty())
    }

    @Test
    fun `скачанный ролик попадает в прочитанный набор`() {
        File(AppPath.r_cache_download, "Creator").apply { mkdirs() }
            .resolve("AbcDef.mp4").writeBytes(ByteArray(16))

        assertEquals(setOf("Creator/AbcDef"), awaitKeys(downloadRed(scope)))
    }
}
