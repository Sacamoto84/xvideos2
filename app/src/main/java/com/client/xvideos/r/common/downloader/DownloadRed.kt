package com.client.xvideos.r.common.downloader

import android.content.Context
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.gallery.GallerySaver
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.r.common.share.useCaseShareGifs
import com.client.xvideos.r.model.GifsInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.SystemPathSeparator
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class RedDownloadRecoveryReport(
    val totalInfoFiles: Int = 0,
    val incompleteItems: Int = 0,
    val queuedVideo: Int = 0,
    val queuedPreview: Int = 0,
    val invalidInfoFiles: Int = 0,
    val skippedNoVideoUrl: Int = 0,
    val skippedNoPreviewUrl: Int = 0
)

private data class RedDownloadRecoveryCandidate(
    val item: GifsInfo
)

@Singleton
class DownloadRed @Inject constructor(
    val downloader: Downloader,
    @ApplicationScope private val scope: CoroutineScope,
    @ApplicationContext private val appContext: Context,
) {

    //var downloadList = mutableStateSetOf<String>()

    private val _downloadList = MutableStateFlow<List<GifsInfo>>(emptyList())
    val downloadList: StateFlow<List<GifsInfo>> = _downloadList.asStateFlow()


    init {
        refreshDownloadList()
    }

    fun downloadItem(item: GifsInfo) {
        scope.launch {
            try {
                Timber.i("Начало загрузки: ${item.id}")
                downloader.downloadRedName(item, onComplete = { refreshDownloadList() })
                Timber.i("Загрузка завершена: ${item.id}")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке: ${item.id}")
            }
        }
    }

    /**
     * Гарантирует наличие файла в кеше: уже скачан — [onReady] сразу,
     * нет — скачиваем и зовём [onReady] по завершению. [onReady] всегда на Main.
     */
    fun ensureDownloaded(item: GifsInfo, onReady: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            if (downloader.findVideoInDownload(item.id, item.userName)) {
                onReady()
            } else {
                downloader.downloadRedName(item, onComplete = {
                    refreshDownloadList()
                    // Колбэк загрузчика приходит не с Main — Toast/Intent/навигация требуют Main.
                    scope.launch(Dispatchers.Main) { onReady() }
                })
            }
        }
    }

    /**
     * «В галерею»: видеофайл (не превью) → /sdcard/xvideos_download.
     * Уже скачан в кеш R — копия; нет — качаем видео напрямую в галерею-папку,
     * мимо кеша (не плодим записи в «Загрузках»).
     */
    fun saveToGallery(item: GifsInfo) {
        if (item.id.isBlank() || item.userName.isBlank()) return
        val fileName = "r_${item.userName}_${item.id}.mp4"

        val local = File("${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4")
        if (local.exists()) {
            GallerySaver.saveLocal(appContext, local, fileName)
            return
        }

        val url = item.downloadVideoUrl()
        if (url == null) {
            SnackBar.error("Нет ссылки на видео")
            return
        }
        GallerySaver.saveFromUrl(appContext, downloader.kDownloader, url, fileName)
    }

    /** «Поделиться»: файл уже в кеше — шарим сразу, иначе скачиваем и шарим по завершению. */
    fun downloadItemAndShare(context: Context, item: GifsInfo) {
        val ctx = context.applicationContext
        ensureDownloaded(item) { useCaseShareGifs(ctx, item) }
    }

    /**
     * P2P для R: бандл — только `.info` (метаданные [GifsInfo]) во временной папке.
     * Получатель добавляет item в свои R Likes (FileDB), превью и видео грузит по URL —
     * физические файлы не передаются вовсе.
     * В r_cache_download ничего не пишем — иначе мусор в «Загрузках» отправителя.
     * [onReady] всегда на Main.
     */
    fun shareMetaByP2p(item: GifsInfo, onReady: (P2pExportBundle) -> Unit) {
        if (item.id.isBlank() || item.userName.isBlank()) return

        scope.launch(Dispatchers.IO) {
            val tmpRoot = File(appContext.cacheDir, "p2p_r_export")
            tmpRoot.deleteRecursively()
            val infoJson = GsonBuilder().create().toJson(item)
            val bundle = buildRMetaBundle(tmpRoot, item.userName, item.id, infoJson)
            withContext(Dispatchers.Main) { onReady(bundle) }
        }
    }

    fun refreshDownloadList() {
        scope.launch(Dispatchers.IO) {
            val rootDir = File(AppPath.r_cache_download)

            val infoFiles = if (rootDir.exists() && rootDir.isDirectory) {
                rootDir.walkTopDown().filter { it.isFile && it.extension == "info" }.toList()
            } else {
                emptyList()
            }

            val gson = GsonBuilder().create()

            val result = mutableListOf<GifsInfo>()

            // Пример обработки каждого файла
            infoFiles.forEach { file ->
                try {
                    val content = file.readText()
                    val obj = gson.fromJson(content, GifsInfo::class.java)
                    result.add(obj)
                } catch (e: Exception) {
                    // Можно логгировать имя файла или путь
                    println("Ошибка при чтении файла ${file.absolutePath}: ${e.message}")
                }
            }

            // Можно отдать список путей или объектов
            _downloadList.emit(result)
        }
    }

    suspend fun scanIncompleteDownloads(): RedDownloadRecoveryReport = withContext(Dispatchers.IO) {
        scanIncompleteDownloadsInternal().report
    }

    fun recoverIncompleteDownloads(
        onComplete: (RedDownloadRecoveryReport) -> Unit = {},
        onEvent: (String) -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            val scan = scanIncompleteDownloadsInternal()
            var report = scan.report
            onEvent("R Download: info ${report.totalInfoFiles}, требуют докачки ${report.incompleteItems}")

            scan.candidates.forEach { candidate ->
                val enqueueReport = downloader.downloadMissingFilesForRecovery(
                    item = candidate.item,
                    onComplete = { refreshDownloadList() },
                    onEvent = onEvent
                )
                report = report.copy(
                    queuedVideo = report.queuedVideo + enqueueReport.queuedVideo,
                    queuedPreview = report.queuedPreview + enqueueReport.queuedPreview,
                    skippedNoVideoUrl = report.skippedNoVideoUrl + enqueueReport.skippedNoVideoUrl,
                    skippedNoPreviewUrl = report.skippedNoPreviewUrl + enqueueReport.skippedNoPreviewUrl
                )
                onEvent(
                    "R Download: ${candidate.item.id} -> видео ${enqueueReport.queuedVideo}, preview ${enqueueReport.queuedPreview}"
                )
            }

            if (report.queuedVideo == 0) {
                refreshDownloadList()
            }

            onComplete(report)
        }
    }

    fun deleteAll(onComplete: () -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            File(AppPath.r_cache_download).deleteRecursively()
            refreshDownloadList()
            onComplete()
        }
    }

    fun delete(item: GifsInfo) {
        scope.launch(Dispatchers.IO) {

            val userDirPath = AppPath.r_cache_download + SystemPathSeparator + item.userName
            val userDir = File(userDirPath)

            val path0 = AppPath.r_cache_download+ SystemPathSeparator + item.userName + SystemPathSeparator + item.id+".mp4"
            val path1 = AppPath.r_cache_download+ SystemPathSeparator + item.userName + SystemPathSeparator + item.id+".info"
            val path2 = AppPath.r_cache_download+ SystemPathSeparator + item.userName + SystemPathSeparator + item.id+".jpg"
            File(path0).delete()
            File(path1).delete()
            File(path2).delete()

            // Проверяем, осталась ли папка пользователя пустой
            if (userDir.exists() && userDir.isDirectory) {
                val files = userDir.listFiles()
                if (files == null || files.isEmpty()) {
                    userDir.delete()  // папка пустая → удаляем
                }
            }

            refreshDownloadList()

            SnackBar.success("Gif удален")
        }
    }

    private data class RecoveryScan(
        val report: RedDownloadRecoveryReport,
        val candidates: List<RedDownloadRecoveryCandidate>
    )

    private fun scanIncompleteDownloadsInternal(): RecoveryScan {
        val rootDir = File(AppPath.r_cache_download)
        val infoFiles = if (rootDir.exists() && rootDir.isDirectory) {
            rootDir.walkTopDown().filter { it.isFile && it.extension.equals("info", ignoreCase = true) }.toList()
        } else {
            emptyList()
        }

        val gson = GsonBuilder().create()
        var invalidInfoFiles = 0
        val candidates = mutableListOf<RedDownloadRecoveryCandidate>()

        infoFiles.forEach { infoFile ->
            runCatching {
                val item = gson.fromJson(infoFile.readText(), GifsInfo::class.java)
                    ?: error("Empty info json")
                val parent = infoFile.parentFile ?: error("Missing parent folder")
                val id = item.id.takeIf { it.isNotBlank() } ?: infoFile.nameWithoutExtension
                val userName = item.userName.takeIf { it.isNotBlank() } ?: parent.name
                val missingVideo = !File(parent, "$id.mp4").exists()
                val missingPreview = !File(parent, "$id.jpg").exists()
                if (missingVideo || missingPreview) {
                    candidates.add(
                        RedDownloadRecoveryCandidate(
                            item = item.copy(id = id, userName = userName)
                        )
                    )
                }
            }.onFailure {
                invalidInfoFiles++
                Timber.e(it, "Ошибка при чтении R Download info: ${infoFile.absolutePath}")
            }
        }

        return RecoveryScan(
            report = RedDownloadRecoveryReport(
                totalInfoFiles = infoFiles.size,
                incompleteItems = candidates.size,
                invalidInfoFiles = invalidInfoFiles
            ),
            candidates = candidates
        )
    }

}
