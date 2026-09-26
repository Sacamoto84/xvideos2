package com.client.xvideos.r.common.downloader

import android.content.Context
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.gallery.GallerySaver
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Отчет о сканировании и восстановлении поврежденных / неполных загрузок RedGifs.
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
 * Фасад управления скачанным контентом RedGifs.
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
 */
@Singleton
class DownloadRed @Inject constructor(
    val downloader: Downloader,
    @ApplicationScope private val scope: CoroutineScope,
    @ApplicationContext private val appContext: Context,
) {

    //var downloadList = mutableStateSetOf<String>()

    private val _downloadList = MutableStateFlow<List<GifsInfo>>(emptyList())
    /** Реактивный список всех сохраненных роликов (сортировка по убыванию даты изменения). */
    val downloadList: StateFlow<List<GifsInfo>> = _downloadList.asStateFlow()

    /**
     * Ключи роликов, у которых на диске лежит готовый `.mp4`.
     *
     * Отдельно от [downloadList]: тот собирается по `.info`-файлам, а `.info`
     * существует и у оборванной закачки без видео. Нужен, чтобы окно
     * предзагрузки ленты не дёргало `File.exists()` на главном потоке для
     * каждого индекса — набор перестраивается тем же обходом, что и список.
     */
    private val _downloadedVideoKeys = MutableStateFlow<Set<String>>(emptySet())
    val downloadedVideoKeys: StateFlow<Set<String>> = _downloadedVideoKeys.asStateFlow()

    /** Проверяет, скачан ли видеофайл ролика по имени автора и id. */
    fun isDownloaded(userName: String, id: String): Boolean =
        downloadedVideoKey(userName, id) in downloadedVideoKeys.value

    /** Проверяет, скачан ли ролик [item]. */
    fun isDownloaded(item: GifsInfo): Boolean =
        isDownloaded(item.userName, item.id)

    /** Количество готовых видеофайлов в локальном хранилище. */
    fun getDownloadedCount(): Int = downloadedVideoKeys.value.size

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
            GallerySaver.saveFromUrl(appContext, downloader.kDownloader, url, fileName, progress = downloader.percent)
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
            val tmpRoot = File(appContext.cacheDir, "p2p_r_export")
            tmpRoot.deleteRecursively()
            val infoJson = AppJson.encodeToString(item)
            val bundle = buildRMetaBundle(tmpRoot, item.userName, item.id, infoJson)
            withContext(Dispatchers.Main) { onReady(bundle) }
        }
    }

    private var refreshJob: Job? = null
    private var recoveryJob: Job? = null

    /**
     * Сканирует каталог `r_cache_download` одним проходом, считывает все `.info` файлы
     * и обновляет [downloadList] и [downloadedVideoKeys].
     */
    fun refreshDownloadList() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            val rootDir = File(AppPath.r_cache_download)
            if (!rootDir.exists() || !rootDir.isDirectory) {
                _downloadList.emit(emptyList())
                _downloadedVideoKeys.emit(emptySet())
                return@launch
            }

            // Один обход на оба результата: собираем .info и .mp4 сразу без
            // промежуточного списка всех файлов и двойной фильтрации.
            val infoFiles = ArrayList<File>()
            val mp4Files = ArrayList<File>()
            for (f in rootDir.walkTopDown()) {
                if (!f.isFile || f.length() <= 0L) continue
                when (f.extension) {
                    "info" -> infoFiles.add(f)
                    "mp4" -> mp4Files.add(f)
                }
            }

            if (infoFiles.isEmpty() && mp4Files.isEmpty()) {
                _downloadList.emit(emptyList())
                _downloadedVideoKeys.emit(emptySet())
                return@launch
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

            _downloadList.emit(result)
            _downloadedVideoKeys.emit(downloadedVideoKeys(mp4Files))
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
     * @param onComplete Вызывается на главном потоке по завершении с итоговым отчетом.
     * @param onEvent Коллбэк прогресса и текстовых событий.
     */
    fun recoverIncompleteDownloads(
        onComplete: (RedDownloadRecoveryReport) -> Unit = {},
        onEvent: (String) -> Unit = {}
    ) {
        recoveryJob?.cancel()
        recoveryJob = scope.launch(Dispatchers.IO) {
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

            withContext(Dispatchers.Main) {
                onComplete(report)
            }
        }
    }

    /**
     * Удаляет все скачанные файлы RedGifs и очищает каталог загрузок.
     */
    fun deleteAll(onComplete: () -> Unit = {}) {
        downloader.kDownloader.cancelAll()
        scope.launch(Dispatchers.IO) {
            File(AppPath.r_cache_download).apply {
                deleteRecursively()
                mkdirs()
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
        scope.launch(Dispatchers.IO) {
            downloader.kDownloader.cancel(item.id)
            val baseDir = File(AppPath.r_cache_download)
            val userDir = File(baseDir, item.userName)
            val fileMp4 = File(userDir, "${item.id}.mp4")
            val fileInfo = File(userDir, "${item.id}.info")
            val fileJpg = File(userDir, "${item.id}.jpg")

            try {
                requireInside(baseDir, userDir)
                requireInside(userDir, fileMp4)
                requireInside(userDir, fileInfo)
                requireInside(userDir, fileJpg)
            } catch (e: Exception) {
                Timber.w(e, "DownloadRed.delete -> Попытка выхода за пределы r_cache_download")
                return@launch
            }

            fileMp4.delete()
            fileInfo.delete()
            fileJpg.delete()

            // Проверяем, осталась ли папка пользователя пустой
            if (userDir.exists() && userDir.isDirectory) {
                val files = userDir.listFiles()
                if (files.isNullOrEmpty()) {
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
 * Ключ скачанного ролика: креатор + id. Формат совпадает с раскладкой кеша
 * `<r_cache_download>/<userName>/<id>.mp4`, поэтому набор ключей строится
 * прямо из списка файлов, без повторного обхода диска.
 */
internal fun downloadedVideoKey(userName: String, id: String): String = "$userName/$id"

/**
 * Набор ключей скачанных видео по списку `.mp4`-файлов кеша.
 *
 * Отдельная функция, а не лямбда внутри обхода: это единственная арифметика в
 * этом пути, и она проверяется обычным JVM-тестом.
 */
internal fun downloadedVideoKeys(videoFiles: List<File>): Set<String> {
    if (videoFiles.isEmpty()) return emptySet()
    return videoFiles.mapTo(HashSet(videoFiles.size)) {
        downloadedVideoKey(it.parentFile?.name.orEmpty(), it.nameWithoutExtension)
    }
}
