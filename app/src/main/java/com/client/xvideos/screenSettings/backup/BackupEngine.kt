package com.client.xvideos.screenSettings.backup

import android.content.Context
import android.net.Uri
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupManager
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.common.backup.XlrBackupReport
import com.client.xvideos.common.backup.XlrBackupType
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.screenSettings.lDownloadRecoveryConsoleText
import com.client.xvideos.screenSettings.redDownloadRecoveryConsoleText
import com.client.xvideos.screenSettings.shouldAutoRecoverL
import com.client.xvideos.screenSettings.shouldAutoRecoverRedDownload
import com.client.xvideos.x.feature.saved.SavedX
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Работа с архивом бэкапа и с сохранённым после восстановления — всё, чем
 * [BackupController] трогает диск. Адрес файла — строкой: держателю не нужен
 * `Uri`, а без него он проверяется обычным тестом.
 */
interface BackupEngine {
    suspend fun currentItems(options: XlrBackupOptions): List<XlrBackupItem>

    /** Пишет зашифрованный архив. Варианта без пароля здесь нет намеренно. */
    suspend fun create(uri: String, paths: Set<String>, options: XlrBackupOptions, password: CharArray): Result<XlrBackupReport>

    suspend fun detectType(uri: String): XlrBackupType

    suspend fun inspect(uri: String, password: CharArray?): Result<List<XlrBackupItem>>

    suspend fun restore(uri: String, paths: Set<String>, password: CharArray?): Result<XlrBackupReport>

    /**
     * Перечитывает сохранённое в память и докачивает медиа восстановленных
     * папок [paths]. Завершается, когда докачка закончена.
     */
    suspend fun afterRestore(paths: Set<String>, log: (String) -> Unit)
}

/** Боевой движок: архивы — через [XlrBackupManager], сохранённое — синглтоны разделов. */
class XlrBackupEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedRed: SavedRed,
    private val blockRed: BlockRed,
    private val downloadRed: DownloadRed,
    private val savedL: SavedL,
    // Lazy: раздел X создаёт свои хранилища при первом входе, и страница
    // бэкапа не должна поднимать их раньше, чем они понадобятся.
    private val savedX: Lazy<SavedX>,
) : BackupEngine {

    override suspend fun currentItems(options: XlrBackupOptions): List<XlrBackupItem> =
        XlrBackupManager.currentBackupItems(options)

    override suspend fun create(
        uri: String,
        paths: Set<String>,
        options: XlrBackupOptions,
        password: CharArray,
    ): Result<XlrBackupReport> = withContext(Dispatchers.IO) {
        XlrBackupManager.createBackup(context, Uri.parse(uri), paths, options, password)
    }

    override suspend fun detectType(uri: String): XlrBackupType = withContext(Dispatchers.IO) {
        XlrBackupManager.detectBackupType(context, Uri.parse(uri))
    }

    override suspend fun inspect(uri: String, password: CharArray?): Result<List<XlrBackupItem>> =
        withContext(Dispatchers.IO) { XlrBackupManager.inspectBackup(context, Uri.parse(uri), password) }

    override suspend fun restore(uri: String, paths: Set<String>, password: CharArray?): Result<XlrBackupReport> =
        withContext(Dispatchers.IO) { XlrBackupManager.restoreBackup(context, Uri.parse(uri), paths, password) }

    override suspend fun afterRestore(paths: Set<String>, log: (String) -> Unit) {
        // Восстановление меняет файлы мимо приложения, а хранилища сохранённого —
        // синглтоны со списками в памяти: диск они читают один раз, при создании.
        // Раньше перечитывался только R. Экраны X свои хранилища не перечитывают,
        // и раздел показывал прежнее до перезапуска; L обновлялся лишь в конце
        // докачки, и то если восстанавливались лайки или коллекции.
        withContext(Dispatchers.IO) {
            savedRed.refreshAll()
            blockRed.refresh()
            downloadRed.refreshDownloadList()
            savedX.get().refreshAll()
            savedL.refreshAll()
        }
        coroutineScope {
            if (shouldAutoRecoverL(paths)) {
                launch {
                    log("L Likes/Collection: сканирую metadata")
                    awaitRecovery { done ->
                        savedL.recoverIncompleteSavedMedia(
                            onEvent = log,
                            onComplete = { report ->
                                log(lDownloadRecoveryConsoleText(report))
                                done()
                            },
                        )
                    }
                }
            }
            if (shouldAutoRecoverRedDownload(paths)) {
                launch {
                    log("R Download: сканирую .info")
                    awaitRecovery { done ->
                        downloadRed.recoverIncompleteDownloads(
                            onEvent = log,
                            onComplete = { report ->
                                log(redDownloadRecoveryConsoleText(report))
                                done()
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Ждёт конца докачки, которая сообщает о нём обратным вызовом. С пределом:
     * прерванная докачка обратный вызов не зовёт, и без предела бэкап остался
     * бы заперт до перезапуска.
     */
    private suspend fun awaitRecovery(start: (done: () -> Unit) -> Unit) {
        val finished = CompletableDeferred<Unit>()
        start { finished.complete(Unit) }
        withTimeoutOrNull(RECOVERY_WAIT_MS) { finished.await() }
    }

    private companion object {
        const val RECOVERY_WAIT_MS = 30L * 60 * 1000
    }
}
