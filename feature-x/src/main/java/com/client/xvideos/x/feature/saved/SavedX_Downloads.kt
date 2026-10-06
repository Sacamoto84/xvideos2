package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Stable
import android.net.Uri
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.gallery.GallerySaver
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parseHTML5Player
import com.client.xvideos.x.parcer.parserItemVideo
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Раздел «Сохранённое» (загрузки) для X.
 *
 * Скачивается оригинальное видео в лучшем качестве — прямой mp4 `videoUrlHigh`
 * (резолвится со страницы видео [ItemsX.href]), плюс картинка превью и `.info`-json
 * с метаданными. Файлы складываются в [AppPath.x_cache_download]. HLS не используется.
 *
 * [percent] питает зелёный индикатор внизу (как в R):
 * `0f..1f` — прогресс, `-2f` — простой/готово, `-3f` — ошибка.
 *
 * `KDownloader` создаётся напрямую через фабрику (DI-модуль в проекте отключён).
 *
 * @property scope CoroutineScope для выполнения сетевых и файловых операций загрузки.
 */
@Stable
class SavedX_Downloads(private val scope: CoroutineScope) {

    private val dir: String = AppPath.x_cache_download
    private val kDownloader by lazy { KDownloader.create(AppContextHolder.applicationContext) }

    /** `0f..1f` — прогресс, `-2f` — простой/готово, `-3f` — ошибка. */
    private val _percent = MutableStateFlow(-2f)
    val percent: StateFlow<Float> = _percent.asStateFlow()

    private val _list = MutableStateFlow<List<ItemsX>>(emptyList())
    val list: StateFlow<List<ItemsX>> = _list.asStateFlow()

    private val _downloadedVideoIds = MutableStateFlow<Set<Long>>(emptySet())
    val downloadedVideoIds: StateFlow<Set<Long>> = _downloadedVideoIds.asStateFlow()

    private val _downloadedPosterIds = MutableStateFlow<Set<Long>>(emptySet())

    // Объявлены до init: он сразу запускает refresh() на другом потоке, и скан
    // мог войти в synchronized раньше, чем замок получил значение.
    private var refreshJob: Job? = null

    /** Сканы каталога идут по одному: см. [loadFromDisk]. */
    private val scanLock = Any()

    init {
        refresh()
    }

    /**
     * O(1) in-memory проверка: файл видео уже сохранён на диске.
     *
     * @param id Числовой ID видео.
     * @return `true`, если ролик скачан и доступен оффлайн.
     */
    val isDownloading: Boolean get() = _percent.value in 0f..1f
    val isIdle: Boolean get() = _percent.value == -2f
    val isError: Boolean get() = _percent.value == -3f
    val isEmpty: Boolean get() = _list.value.isEmpty()
    val isNotEmpty: Boolean get() = _list.value.isNotEmpty()
    val count: Int get() = _list.value.size

    fun contains(id: Long): Boolean = id > 0L && _downloadedVideoIds.value.contains(id)
    fun contains(item: ItemsX?): Boolean = item != null && contains(item.id)

    /** id роликов, которые сейчас качаются: от нажатия «Скачать» до конца загрузки. */
    private val inFlight = HashSet<Long>()

    /**
     * Прогресс каждой идущей загрузки по id. [percent] собирается из всех сразу:
     * параллельные загрузки не перетирают друг друга, а окончание одной не
     * выставляет «готово», пока идут остальные.
     */
    private val activeProgress = HashMap<Long, Float>()

    /** Регистрирует загрузку ролика; false — он уже качается. */
    internal fun markStarted(id: Long): Boolean = synchronized(inFlight) { inFlight.add(id) }

    internal fun onVideoProgress(id: Long, value: Float) = synchronized(inFlight) {
        if (id !in inFlight) return@synchronized
        activeProgress[id] = value
        _percent.value = activeProgress.values.average().toFloat()
    }

    /** Завершает учёт загрузки; поздние колбэки уже снятой загрузки игнорируются. */
    internal fun onVideoFinished(id: Long, failed: Boolean) = synchronized(inFlight) {
        if (!inFlight.remove(id)) return@synchronized
        activeProgress.remove(id)
        _percent.value = when {
            activeProgress.isNotEmpty() -> activeProgress.values.average().toFloat()
            failed -> -3f
            else -> -2f
        }
    }

    /** Состояние до постановки в очередь: меняем, только если ничего не качается. */
    private fun setIdleState(value: Float) = synchronized(inFlight) {
        if (activeProgress.isEmpty()) _percent.value = value
    }

    /**
     * Возвращает `file://`-URI скачанного видеофайла (для передачи в ExoPlayer).
     *
     * @param id Числовой ID видео.
     */
    fun localUrl(id: Long): String = Uri.fromFile(File(dir, "$id.mp4")).toString()

    /**
     * Абсолютный путь к локальной картинке превью (`<id>.jpg`), если она скачана, иначе null.
     * UrlImage сам грузит локальный файл, если строка не начинается с `https://`.
     *
     * @param id Числовой ID видео.
     */
    fun localPosterPath(id: Long): String? {
        return if (id > 0L && _downloadedPosterIds.value.contains(id)) File(dir, "$id.jpg").absolutePath else null
    }

    /**
     * Скачать оригинальное видео в наилучшем качестве (`videoUrlHigh`, прямой mp4)
     * в [AppPath.x_cache_download].
     *
     * Прямой URL берётся со страницы видео ([ItemsX.href]) тем же путём, что и плеер:
     * `readHtmlFromURLDirect` → [parserItemVideo] → [parseHTML5Player]. HLS не нужен.
     * Прогресс отражается в [percent], по завершении пишется `.info` и шлётся снекбар.
     *
     * @param item Объект ролика для сохранения.
     */
    fun download(item: ItemsX) {
        if (item.id <= 0L) return
        if (contains(item.id)) {
            SnackBar.info("Уже сохранено")
            return
        }
        // Прямая ссылка резолвится заново на каждое нажатие и каждый раз другая
        // (токен), поэтому KDownloader дубль не узнает — отсекаем его здесь.
        if (!markStarted(item.id)) {
            SnackBar.info("Уже скачивается")
            return
        }

        setIdleState(-2f)
        SnackBar.info("Получение ссылки на видео…")

        scope.launch(Dispatchers.IO) {
            try {
                enqueueVideo(item)
            } catch (e: CancellationException) {
                onVideoFinished(item.id, failed = true)
                throw e
            } catch (e: Exception) {
                Timber.e("X download: не удалось поставить в очередь ${item.id}: ${e.javaClass.simpleName}")
                onVideoFinished(item.id, failed = true)
                SnackBar.error("Ошибка скачивания: ${e.message}")
            }
        }
    }

    /** Резолвит прямую ссылку и ставит видео (и превью) в очередь загрузчика. */
    private suspend fun enqueueVideo(item: ItemsX) {
        val videoUrl = resolveDirectVideoUrl(item)

        if (videoUrl.isNullOrBlank()) {
            onVideoFinished(item.id, failed = true)
            SnackBar.error("Не удалось получить ссылку на видео")
            return
        }

        File(dir).mkdirs()

        // Превью-картинка (необязательно — ошибки не критичны).
        if (item.previewImage.isNotBlank()) {
            runCatching {
                val reqImg = kDownloader.newRequestBuilder(item.previewImage, dir, "${item.id}.jpg")
                    .tag(item.id.toString())
                    .build()
                kDownloader.enqueue(reqImg)
            }
        }

        val req = kDownloader.newRequestBuilder(videoUrl, dir, "${item.id}.mp4")
            .tag(item.id.toString())
            .build()

        kDownloader.enqueue(
            req,
            onStart = { onVideoProgress(item.id, 0f) },
            onProgress = { p -> onVideoProgress(item.id, p / 100f) },
            onError = {
                // При сбое сети текст — toString() исключения: после двоеточия в нём хост.
                Timber.e("X download error ${item.id}: ${it.substringBefore(':')}")
                onVideoFinished(item.id, failed = true)
                SnackBar.error("Ошибка скачивания: $it")
            },
            onCompleted = { scope.launch(Dispatchers.IO) { completeDownload(item) } },
            // Отмена — не сбой: загрузку сняли удалением ролика.
            onCancelled = { onVideoFinished(item.id, failed = false) },
        )
    }

    /** Завершает загрузку [item]: видео уже на диске, осталось записать `.info`. */
    internal fun completeDownload(item: ItemsX) {
        val file = File(dir, "${item.id}.mp4")
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            onVideoFinished(item.id, failed = true)
            SnackBar.error("Ошибка: скачанный файл пуст")
            return
        }
        // `.info` — до объявления успеха. Раньше «Скачано» показывалось первым,
        // а сбой записи только логировался: видео оставалось на диске без
        // `.info` — в списке его нет, удалить из приложения нельзя.
        val infoWritten = runCatching {
            File(dir, "${item.id}.info").writeTextAtomically(AppJson.encodeToString(item))
        }.onFailure { Timber.e(it, "X download: ошибка записи .info ${item.id}") }.isSuccess
        if (!infoWritten) {
            file.delete()
            File(dir, "${item.id}.jpg").delete()
            onVideoFinished(item.id, failed = true)
            SnackBar.error("Ошибка: не удалось сохранить данные ролика")
            return
        }
        onVideoFinished(item.id, failed = false)
        SnackBar.success("Скачано")
        loadFromDisk()
    }

    /**
     * Резолвит прямой mp4 наилучшего качества со страницы видео ([ItemsX.href]) —
     * тем же путём, что и плеер: `readHtmlFromURLDirect` → [parserItemVideo] →
     * [parseHTML5Player]. Возвращает null, если не удалось извлечь ссылку.
     *
     * @param item Объект ролика.
     * @return Прямая ссылка на MP4 файл наивысшего качества.
     */
    suspend fun resolveDirectVideoUrl(item: ItemsX): String? = runCatchingCancellable {
        val pageUrl = normalizeXUrl(item.href)
        val html = readHtmlFromURLDirect(pageUrl)
        val config = parserItemVideo(html)?.let { parseHTML5Player(it) }
        config?.videoUrlHigh?.takeIf { it.isNotBlank() }
            ?: config?.videoUrlLow?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /**
     * Сохранение видео в общую галерею устройства: если видео уже скачано в кэш —
     * копируется в общую галерею через [GallerySaver.saveLocal],
     * иначе резолвится прямой mp4 и скачивается напрямую туда через [GallerySaver.saveFromUrl].
     *
     * @param item Объект сохраняемого ролика.
     */
    fun saveToGallery(item: ItemsX) {
        if (item.id <= 0L) return
        val context = AppContextHolder.applicationContext
        val fileName = "x_${item.id}.mp4"

        scope.launch(Dispatchers.IO) {
            val local = File(dir, "${item.id}.mp4")
            if (local.exists() && local.length() > 0L) {
                GallerySaver.saveLocal(context, local, fileName)
                return@launch
            }

            // Сохранение идёт через тот же учёт, что и загрузки: раньше оно
            // писало в percent напрямую, и при одновременной загрузке индикатор
            // скакал между двумя источниками. Заодно тот же ролик не качается дважды.
            if (!markStarted(item.id)) {
                SnackBar.info("Уже скачивается")
                return@launch
            }
            SnackBar.info("Получение ссылки на видео…")
            val videoUrl = try {
                resolveDirectVideoUrl(item)
            } catch (e: CancellationException) {
                onVideoFinished(item.id, failed = false)
                throw e
            }
            if (videoUrl.isNullOrBlank()) {
                onVideoFinished(item.id, failed = true)
                SnackBar.error("Не удалось получить ссылку на видео")
                return@launch
            }
            GallerySaver.saveFromUrl(
                context = context,
                kDownloader = kDownloader,
                url = videoUrl,
                fileName = fileName,
                onProgress = { onVideoProgress(item.id, it) },
                onFinished = { failed -> onVideoFinished(item.id, failed) },
            )
        }
    }

    /**
     * Удаляет скачанный видеоролик, его обложку и файл метаданных `.info` из локального диска.
     *
     * @param item Удаляемый ролик.
     */
    fun delete(item: ItemsX) {
        if (item.id <= 0L) return
        scope.launch(Dispatchers.IO) {
            kDownloader.cancel(item.id.toString())
            // Отменённая загрузка не должна держать id «качается» и общий прогресс.
            onVideoFinished(item.id, failed = false)
            File(dir, "${item.id}.mp4").delete()
            File(dir, "${item.id}.jpg").delete()
            File(dir, "${item.id}.info").delete()
            loadFromDisk()
            SnackBar.info("Удалено из сохранённого")
        }
    }

    /**
     * Перечитать список сохранённого по `.info`-файлам на диске.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            loadFromDisk()
        }
    }

    /**
     * Сканирует директорию [dir], сопоставляет mp4, jpg и info файлы, формируя актуальный список загрузок.
     *
     * Скан и публикация результата — под одним замком. Метод зовут из разных
     * корутин (обновление, удаление, конец каждой загрузки), и без замка скан,
     * начатый раньше, мог опубликовать список позже свежего: только что
     * скачанный ролик пропадал из списка до следующего обновления.
     */
    private fun loadFromDisk() = synchronized(scanLock) { scanAndPublish() }

    private fun scanAndPublish() {
        val root = File(dir)
        val allFiles = if (root.exists() && root.isDirectory) {
            root.listFiles() ?: emptyArray()
        } else {
            emptyArray()
        }

        if (allFiles.isEmpty()) {
            if (_list.value.isNotEmpty()) {
                _downloadedVideoIds.value = emptySet()
                _downloadedPosterIds.value = emptySet()
                _list.value = emptyList()
            }
            return
        }

        val videoIds = HashSet<Long>()
        val posterIds = HashSet<Long>()
        val infos = ArrayList<File>()

        for (file in allFiles) {
            if (!file.isFile || file.length() == 0L) continue
            when (file.extension) {
                "mp4" -> file.nameWithoutExtension.toLongOrNull()?.let { videoIds.add(it) }
                "jpg" -> file.nameWithoutExtension.toLongOrNull()?.let { posterIds.add(it) }
                "info" -> infos.add(file)
            }
        }
        infos.sortByDescending { it.lastModified() }

        val result = ArrayList<ItemsX>(infos.size)
        for (f in infos) {
            val item = runCatching { AppJson.decodeFromString<ItemsX>(f.readText()) }
                .onFailure { Timber.e(it, "X saved: битый .info ${f.absolutePath}") }
                .getOrNull()
            if (item != null && item.id in videoIds) {
                result.add(item)
            }
        }
        // Сохранённым считается только ролик из списка: видео без `.info`
        // в список не попадает, и числиться сохранённым не должно — иначе его
        // не видно и нельзя ни удалить, ни скачать заново.
        _downloadedVideoIds.value = result.mapTo(HashSet(result.size)) { it.id }
        _downloadedPosterIds.value = posterIds
        _list.value = result
    }
}
