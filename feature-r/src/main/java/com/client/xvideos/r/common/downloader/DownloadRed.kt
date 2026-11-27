package com.client.xvideos.r.common.downloader

import android.content.Context
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.gallery.GallerySaver
import com.client.xvideos.common.kdownloader.utils.getTempPath
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.r.common.share.useCaseShareGifs
import com.client.xvideos.r.model.GifsInfo
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отчет о сканировании и восстановлении поврежденных / неполных загрузок R.
 *
 * @property totalInfoFiles Всего найдено `.info` файлов на диске.
 * @property incompleteItems Количество элементов с отсутствующим видео или превью.
 * @property queuedVideo Количество видеофайлов, поставленных в очередь докачки.
 * @property queuedPreview Количество превью-картинок, поставленных в очередь докачки.
 * @property invalidInfoFiles Число поврежденных `.info` файлов, которые не удалось распарсить.
 * @property skippedNoVideoUrl Пропущено из-за отсутствия ссылки на видео.
 * @property skippedNoPreviewUrl Пропущено из-за отсутствия ссылки на превью.
 */
@Immutable
data class RedDownloadRecoveryReport(
    val totalInfoFiles: Int = 0,
    val incompleteItems: Int = 0,
    val queuedVideo: Int = 0,
    val queuedPreview: Int = 0,
    val invalidInfoFiles: Int = 0,
    val skippedNoVideoUrl: Int = 0,
    val skippedNoPreviewUrl: Int = 0
) {
    /** Суммарное количество файлов, поставленных в очередь докачки. */
    val totalQueued: Int get() = queuedVideo + queuedPreview

    /** Суммарное количество пропущенных файлов. */
    val totalSkipped: Int get() = skippedNoVideoUrl + skippedNoPreviewUrl

    /** Флаг наличия неполных медиаэлементов. */
    val hasIncomplete: Boolean get() = incompleteItems > 0

    /** Флаг наличия поврежденных .info файлов. */
    val hasErrors: Boolean get() = invalidInfoFiles > 0

    /** Флаг абсолютной целостности и полноты всех скачанных элементов. */
    val isClean: Boolean get() = incompleteItems == 0 && invalidInfoFiles == 0
}

/** Кандидат на восстановление загрузки (элемент с отсутствующим `.mp4` или `.jpg`). */
private data class RedDownloadRecoveryCandidate(
    val item: GifsInfo
)

/**
 * Фасад управления скачанным контентом R.
 *
 * Предоставляет:
 * - Реактивный список всех загруженных элементов [downloadList] (построенный по метаданным `.info`);
 * - Набор ключей физически присутствующих видеофайлов [downloadedVideoKeys] (для быстрого O(1) поиска в UI плеера без I/O на главном потоке);
 * - Сохранение в общую галерею устройства [saveToGallery];
 * - Шеринг медиа через системный Intent [downloadItemAndShare];
 * - Экспорт метаданных для P2P-передачи [shareMetaByP2p];
 * - Проверку и автоматическое восстановление незавершенных загрузок [recoverIncompleteDownloads];
 * - Удаление одиночных файлов [delete] и полной очистки папки загрузок [deleteAll].
 *
 * @property downloader Низкоуровневый загрузчик файлов.
 * @param scope Скоп приложения для долгоживущих операций.
 * @param appContext Контекст приложения.
 * @param readDownloads Чтение каталога загрузок; тест подменяет его, чтобы
 * управлять порядком обходов.
 */
@Singleton
class DownloadRed internal constructor(
    val downloader: Downloader,
    private val scope: CoroutineScope,
    private val appContext: Context,
    private val readDownloads: () -> RedDownloadsScan,
) {
    @Inject
    constructor(
        downloader: Downloader,
        @ApplicationScope scope: CoroutineScope,
        @ApplicationContext appContext: Context,
    ) : this(downloader, scope, appContext, { readRedDownloads(File(AppPath.r_cache_download)) })

    //var downloadList = mutableStateSetOf<String>()

    private val _downloadList = MutableStateFlow<List<GifsInfo>>(emptyList())
    /** Реактивный список всех сохраненных роликов (сортировка по убыванию даты изменения). */
    val downloadList: StateFlow<List<GifsInfo>> = _downloadList.asStateFlow()

    private val _downloads = MutableStateFlow(RedDownloadsScan(emptyList(), emptyMap()))
    internal val downloads = _downloads.asStateFlow()

    /**
     * Ключи роликов, у которых на диске лежит готовый `.mp4`.
     *
     * Отдельно от [downloadList]: тот собирается по `.info`-файлам, а `.info`
     * существует и у оборванной закачки без видео. Нужен, чтобы окно
     * предзагрузки ленты не дёргало `File.exists()` на главном потоке для
     * каждого индекса — набор перестраивается тем же обходом, что и список.
     *
     * `null` — каталог ещё не прочитан: первый обход идёт из `init` на IO, а
     * экран, создавший этот синглтон (полноэкранный R после смерти процесса),
     * компонуется раньше, чем он закончится. Пустой набор вместо `null` нельзя
     * было отличить от «скачанного нет», и страница навсегда запоминала адрес
     * HLS у скачанного ролика (проход 208, C1).
     */
    private val _downloadedVideoKeys = MutableStateFlow<Set<String>?>(null)
    val downloadedVideoKeys: StateFlow<Set<String>?> = _downloadedVideoKeys.asStateFlow()

    /** Проверяет, скачан ли видеофайл ролика по имени автора и id. */
    fun isDownloaded(userName: String, id: String): Boolean =
        downloadedVideoKeys.value?.contains(downloadedVideoKey(userName, id)) == true

    /** Проверяет, скачан ли ролик [item]. */
    fun isDownloaded(item: GifsInfo): Boolean =
        isDownloaded(item.userName, item.id)

    /** Количество готовых видеофайлов в локальном хранилище. */
    fun getDownloadedCount(): Int = downloadedVideoKeys.value?.size ?: 0

    /**
     * Обходы каталога идут по одному: чтение и публикация — под одним замком,
     * как в `BlockRed`. Отмена прежнего обхода его не останавливает (в обходе
     * нет точек приостановки, а `emit` отмену не проверяет), и без очереди
     * ранний обход, закончив позже, клал устаревший список поверх свежего
     * (проход 209, T1). Объявлен до `init`: первый обход запускается оттуда.
     */
    private val refreshMutex = Mutex()

    init {
        refreshDownloadList()
    }

    /**
     * Инициирует загрузку медиаэлемента [item] с последующим обновлением списка.
     */
    fun downloadItem(item: GifsInfo) {
        if (item.id.isBlank() || item.userName.isBlank()) {
            Timber.w("Skip downloadItem with blank id or userName: id=${item.id}, user=${item.userName}")
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                Timber.i("Начало загрузки: ${item.id}")
                downloader.downloadRedName(item, onComplete = { refreshDownloadList() })
                Timber.i("Загрузка завершена: ${item.id}")
            } catch (e: CancellationException) {
                throw e
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
            val exists = withContext(Dispatchers.IO) {
                downloader.findVideoInDownload(item.id, item.userName)
            }
            if (exists) {
                onReady()
            } else {
                withContext(Dispatchers.IO) {
                    downloader.downloadRedName(item, onComplete = {
                        refreshDownloadList()
                        // Колбэк загрузчика приходит не с Main — Toast/Intent/навигация требуют Main.
                        scope.launch(Dispatchers.Main) { onReady() }
                    })
                }
            }
        }
    }

    /**
     * «В галерею»: видеофайл (не превью) → общая галерея.
     * Уже скачан в кеш R — копия; нет — качаем видео напрямую в галерею-папку,
     * мимо кеша (не плодим записи в «Загрузках»).
     */
    fun saveToGallery(item: GifsInfo) {
        if (item.id.isBlank() || item.userName.isBlank()) return
        if (isUnsafeItemName(item.userName) || isUnsafeItemName(item.id)) {
            Timber.w("DownloadRed.saveToGallery -> Отклонён небезопасный путь: userName=${item.userName}, id=${item.id}")
            return
        }

        val fileName = "r_${item.userName}_${item.id}.mp4"

        scope.launch(Dispatchers.IO) {
            val baseDir = File(AppPath.r_cache_download)
            val userDir = File(baseDir, item.userName)
            val local = File(userDir, "${item.id}.mp4")
            try {
                requireInside(baseDir, userDir)
                requireInside(userDir, local)
            } catch (e: Exception) {
                Timber.w(e, "DownloadRed.saveToGallery -> Попытка выхода за пределы r_cache_download")
                return@launch
            }

            if (local.exists() && local.length() > 0L) {
                GallerySaver.saveLocal(appContext, local, fileName)
                return@launch
            }

            val url = item.downloadVideoUrl()
            if (url == null) {
                SnackBar.error("Нет ссылки на видео")
                return@launch
            }
            // Прогресс — через общий учёт загрузок, а не записью в percent мимо
            // него: иначе индикатор скакал между загрузкой и сохранением.
            // Ключ отдельный от id ролика: тот же ролик может качаться в кеш.
            val progressKey = "gallery/${item.id}"
            GallerySaver.saveFromUrl(
                context = appContext,
                kDownloader = downloader.kDownloader,
                url = url,
                fileName = fileName,
                onProgress = { downloader.onVideoProgress(progressKey, it) },
                onFinished = { failed -> downloader.onVideoFinished(progressKey, failed) },
            )
        }
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
        if (isUnsafeItemName(item.userName) || isUnsafeItemName(item.id)) return

        scope.launch(Dispatchers.IO) {
            val exportRoot = File(appContext.cacheDir, "p2p_r_export")
            cleanupStaleP2pExports(exportRoot)
            val tmpRoot = File(exportRoot, "${System.currentTimeMillis()}_${UUID.randomUUID()}")
            val infoJson = AppJson.encodeToString(item)
            val bundle = buildRMetaBundle(tmpRoot, item.userName, item.id, infoJson)
            withContext(Dispatchers.Main) { onReady(bundle) }
        }
    }

    private fun cleanupStaleP2pExports(exportRoot: File, nowMs: Long = System.currentTimeMillis()) {
        val children = exportRoot.listFiles() ?: return
        val staleCutoffMs = nowMs - STALE_P2P_EXPORT_AGE_MS
        for (child in children) {
            if (child.lastModified() <= staleCutoffMs) {
                child.deleteRecursively()
            }
        }
    }

    private companion object {
        const val STALE_P2P_EXPORT_AGE_MS = 60L * 60 * 1000
    }

    private var refreshJob: Job? = null
    private val recoveryMutex = Mutex()

    /**
     * Сканирует каталог `r_cache_download` одним проходом, считывает все `.info` файлы
     * и обновляет данные загрузок, включая размеры видео.
     */
    fun refreshDownloadList() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            refreshMutex.withLock {
                val scan = readDownloads()
                _downloads.emit(scan)
                _downloadList.emit(scan.list)
                _downloadedVideoKeys.emit(scan.videoKeys)
            }
        }.apply {
            // Сбой обхода ловит обработчик области приложения, и без этого набор
            // так и остался бы непрочитанным: полноэкранный R ждал бы его вечно.
            // Отмену пропускаем — обход прерывает следующий, и набор отдаст он.
            invokeOnCompletion { cause ->
                if (cause != null && cause !is CancellationException) {
                    _downloadedVideoKeys.update { it ?: emptySet() }
                }
            }
        }
    }

    /**
     * Сканирует каталог на наличие неполных закачек и формирует [RedDownloadRecoveryReport].
     */
    suspend fun scanIncompleteDownloads(): RedDownloadRecoveryReport = withContext(Dispatchers.IO) {
        scanIncompleteDownloadsInternal().report
    }

    /**
     * Находит все неполные загрузки и докачивает недостающие видео или превью.
     *
     * @param onEvent Коллбэк прогресса и текстовых событий.
     */
    suspend fun recoverIncompleteDownloads(
        onEvent: (String) -> Unit = {}
    ): RedDownloadRecoveryReport = withContext(Dispatchers.IO) {
        recoveryMutex.withLock {
            // Загрузки принадлежат KDownloader и продолжаются при закрытии экрана.
            withContext(NonCancellable) { recoverDownloads(onEvent) }
        }
    }

    private suspend fun recoverDownloads(onEvent: (String) -> Unit): RedDownloadRecoveryReport {
        val scan = scanIncompleteDownloadsInternal()
        var report = scan.report
        onEvent("R Download: info ${report.totalInfoFiles}, требуют докачки ${report.incompleteItems}")

        val pending = mutableListOf<Deferred<Unit>>()
        try {
            scan.candidates.forEach { candidate ->
                val enqueueReport = downloader.downloadMissingFilesForRecovery(
                    item = candidate.item,
                    onComplete = {},
                    onEvent = onEvent,
                    pending = pending
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
        } finally {
            pending.joinAll()
            refreshDownloadList()
        }
        return report
    }

    /**
     * Удаляет все скачанные файлы R и очищает каталог загрузок.
     */
    fun deleteAll(onComplete: () -> Unit = {}) {
        downloader.invalidateAll()
        scope.launch(Dispatchers.IO) {
            downloader.kDownloader.cancelAllAndJoin()
            downloader.joinAllCompletionJobs()
            downloader.withCacheLock {
                File(AppPath.r_cache_download).apply {
                    deleteRecursively()
                    mkdirs()
                }
            }
            refreshDownloadList()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Удаляет конкретный скачанный элемент [item] (mp4, info, jpg),
     * а также папку автора, если она осталась пустой.
     */
    fun delete(item: GifsInfo) {
        val userName = item.userName
        val id = item.id
        if (userName.isBlank() || id.isBlank()) return
        if (isUnsafeItemName(userName) || isUnsafeItemName(id)) {
            Timber.w("DownloadRed.delete -> Отклонён небезопасный путь: userName=$userName, id=$id")
            return
        }
        downloader.invalidateItem(id)
        scope.launch(Dispatchers.IO) {
            downloader.kDownloader.cancelAndJoin(id)
            downloader.joinCompletionJobs(id)
            val baseDir = File(AppPath.r_cache_download)
            val userDir = File(baseDir, item.userName)
            val files = downloadedItemFiles(userDir, item.id)

            try {
                requireInside(baseDir, userDir)
                files.forEach { requireInside(userDir, it) }
            } catch (e: Exception) {
                Timber.w(e, "DownloadRed.delete -> Попытка выхода за пределы r_cache_download")
                return@launch
            }

            downloader.withCacheLock {
                files.forEach { it.delete() }

                // Проверяем, осталась ли папка пользователя пустой
                if (userDir.exists() && userDir.isDirectory) {
                    val remaining = userDir.listFiles()
                    if (remaining.isNullOrEmpty()) {
                        userDir.delete()  // папка пустая → удаляем
                    }
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

    /**
     * Внутренний метод анализа дискового пространства загрузок и поиска неполных записей.
     */
    private fun scanIncompleteDownloadsInternal(): RecoveryScan {
        val rootDir = File(AppPath.r_cache_download)
        val infoFiles = if (rootDir.exists() && rootDir.isDirectory) {
            rootDir.walkTopDown().filter { it.isFile && it.extension.equals("info", ignoreCase = true) }.toList()
        } else {
            emptyList()
        }

        var invalidInfoFiles = 0
        val candidates = mutableListOf<RedDownloadRecoveryCandidate>()

        infoFiles.forEach { infoFile ->
            runCatching {
                val item = AppJson.decodeFromString<GifsInfo>(infoFile.readText())
                val parent = infoFile.parentFile ?: error("Missing parent folder")
                val id = item.id.takeIf { it.isNotBlank() } ?: infoFile.nameWithoutExtension
                val userName = item.userName.takeIf { it.isNotBlank() } ?: parent.name
                val missingVideo = !File(parent, "$id.mp4").let { it.exists() && it.length() > 0L }
                val missingPreview = !File(parent, "$id.jpg").let { it.exists() && it.length() > 0L }
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

/**
 * Файлы скачанного ролика, которые уходят при его удалении: видео, обложка,
 * `.info` и остатки их оборванной загрузки. Загрузчик качает во временный файл
 * рядом; без этого остаток повторной загрузки оставался на диске без записи в
 * списке.
 */
internal fun downloadedItemFiles(userDir: File, id: String): List<File> {
    val downloaded = listOf("$id.mp4", "$id.jpg")
    return downloaded.map { File(userDir, it) } +
        downloaded.map { File(getTempPath(userDir.path, it)) } +
        File(userDir, "$id.info")
}

/**
 * Ключ скачанного ролика: креатор + id. Формат совпадает с раскладкой кеша
 * `<r_cache_download>/<userName>/<id>.mp4`, поэтому набор ключей строится
 * прямо из файлов при обходе диска.
 */
internal fun downloadedVideoKey(userName: String, id: String): String = "$userName/$id"

internal fun downloadedVideoKey(file: File): String =
    downloadedVideoKey(file.parentFile?.name.orEmpty(), file.nameWithoutExtension)

/** Ролики по `.info` и размеры готовых `.mp4` из одного обхода каталога. */
internal data class RedDownloadsScan(val list: List<GifsInfo>, val videoSizes: Map<String, Long>) {
    val videoKeys: Set<String> get() = videoSizes.keys

    fun videoSize(item: GifsInfo): Long? = videoSizes[downloadedVideoKey(item.userName, item.id)]
}

/**
 * Собирает `.info` и размеры `.mp4` за один обход [rootDir].
 * Список упорядочен по дате изменения `.info`, повреждённые записи пропускаются.
 */
internal fun readRedDownloads(rootDir: File): RedDownloadsScan {
    if (!rootDir.isDirectory) return RedDownloadsScan(emptyList(), emptyMap())

    val infoFiles = ArrayList<File>()
    val videoSizes = HashMap<String, Long>()
    for (f in rootDir.walkTopDown()) {
        if (!f.isFile) continue
        val size = f.length()
        if (size <= 0L) continue
        when (f.extension) {
            "info" -> infoFiles.add(f)
            "mp4" -> videoSizes[downloadedVideoKey(f)] = size
        }
    }

    if (infoFiles.size > 1) {
        infoFiles.sortByDescending { it.lastModified() }
    }
    val result = ArrayList<GifsInfo>(infoFiles.size)
    for (file in infoFiles) {
        try {
            val content = file.readText()
            val obj = AppJson.decodeFromString<GifsInfo>(content)
            result.add(obj)
        } catch (e: Exception) {
            // Битый .info пропускаем, но в лог приложения, а не в stdout.
            Timber.w(e, "Ошибка при чтении файла ${file.absolutePath}")
        }
    }

    return RedDownloadsScan(result, videoSizes)
}
