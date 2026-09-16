package com.client.xvideos.x.feature.saved

import android.net.Uri
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.gallery.GallerySaver
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.x.urlStart
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parseHTML5Player
import com.client.xvideos.x.parcer.parserItemVideo
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 */
class SavedX_Downloads(private val scope: CoroutineScope) {

    private val dir: String = AppPath.x_cache_download
    private val kDownloader by lazy { KDownloader.create(AppContextHolder.applicationContext) }

    /** `0f..1f` — прогресс, `-2f` — простой/готово, `-3f` — ошибка. */
    val percent = MutableStateFlow(-2f)

    private val _list = MutableStateFlow<List<ItemsX>>(emptyList())
    val list: StateFlow<List<ItemsX>> = _list.asStateFlow()

    private val _downloadedVideoIds = MutableStateFlow<Set<Long>>(emptySet())
    val downloadedVideoIds: StateFlow<Set<Long>> = _downloadedVideoIds.asStateFlow()

    private val _downloadedPosterIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        refresh()
    }

    /** O(1) in-memory проверка: файл видео уже сохранён. */
    fun contains(id: Long): Boolean = id > 0L && _downloadedVideoIds.value.contains(id)

    /** `file://`-URI скачанного видео (для ExoPlayer). */
    fun localUrl(id: Long): String = Uri.fromFile(File(dir, "$id.mp4")).toString()

    /**
     * Абсолютный путь к локальной картинке превью (`<id>.jpg`), если она скачана, иначе null.
     * UrlImage сам грузит локальный файл, если строка не начинается с `https://`.
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
     */
    fun download(item: ItemsX) {
        if (item.id <= 0L) return
        if (contains(item.id)) {
            SnackBar.info("Уже сохранено")
            return
        }

        percent.value = -2f
        SnackBar.info("Получение ссылки на видео…")

        scope.launch(Dispatchers.IO) {
            val videoUrl = resolveDirectVideoUrl(item)

            if (videoUrl.isNullOrBlank()) {
                percent.value = -3f
                SnackBar.error("Не удалось получить ссылку на видео")
                return@launch
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
                onStart = { percent.value = 0f },
                onProgress = { p -> percent.value = p / 100f },
                onError = {
                    Timber.e("X download error ${item.id}: $it")
                    percent.value = -3f
                    SnackBar.error("Ошибка скачивания: $it")
                },
                onCompleted = {
                    scope.launch(Dispatchers.IO) {
                        val file = File(dir, "${item.id}.mp4")
                        if (!file.exists() || file.length() == 0L) {
                            file.delete()
                            percent.value = -3f
                            SnackBar.error("Ошибка: скачанный файл пуст")
                            return@launch
                        }
                        percent.value = -2f
                        SnackBar.success("Скачано")
                        runCatching {
                            File(dir, "${item.id}.info").writeTextAtomically(AppJson.encodeToString(item))
                        }.onFailure { Timber.e(it, "X download: ошибка записи .info ${item.id}") }
                        loadFromDisk()
                    }
                },
            )
        }
    }

    /**
     * Прямой mp4 наилучшего качества со страницы видео ([ItemsX.href]) —
     * тем же путём, что и плеер: `readHtmlFromURLDirect` → [parserItemVideo] →
     * [parseHTML5Player]. null, если не удалось.
     */
    suspend fun resolveDirectVideoUrl(item: ItemsX): String? = runCatchingCancellable {
        val pageUrl = if (item.href.startsWith("http")) item.href else urlStart + item.href
        val html = readHtmlFromURLDirect(pageUrl)
        val config = parserItemVideo(html)?.let { parseHTML5Player(it) }
        config?.videoUrlHigh?.takeIf { it.isNotBlank() }
            ?: config?.videoUrlLow?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /**
     * «В галерею»: видео уже в кеше — копия в общую галерею,
     * иначе резолвим прямой mp4 и качаем туда целиком.
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

            SnackBar.info("Получение ссылки на видео…")
            val videoUrl = resolveDirectVideoUrl(item)
            if (videoUrl.isNullOrBlank()) {
                SnackBar.error("Не удалось получить ссылку на видео")
                return@launch
            }
            GallerySaver.saveFromUrl(context, kDownloader, videoUrl, fileName, progress = percent)
        }
    }

    fun delete(item: ItemsX) {
        if (item.id <= 0L) return
        scope.launch(Dispatchers.IO) {
            kDownloader.cancel(item.id.toString())
            File(dir, "${item.id}.mp4").delete()
            File(dir, "${item.id}.jpg").delete()
            File(dir, "${item.id}.info").delete()
            loadFromDisk()
            SnackBar.info("Удалено из сохранённого")
        }
    }

    /** Перечитать список сохранённого по `.info`-файлам. */
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            loadFromDisk()
        }
    }

    private fun loadFromDisk() {
        val root = File(dir)
        val allFiles = if (root.exists() && root.isDirectory) {
            root.listFiles() ?: emptyArray()
        } else {
            emptyArray()
        }

        val videoIds = allFiles.filter { it.isFile && it.extension == "mp4" && it.length() > 0L }
            .mapNotNull { it.nameWithoutExtension.toLongOrNull() }.toSet()
        val posterIds = allFiles.filter { it.isFile && it.extension == "jpg" && it.length() > 0L }
            .mapNotNull { it.nameWithoutExtension.toLongOrNull() }.toSet()

        val infos = allFiles.filter { it.isFile && it.extension == "info" && it.length() > 0L }
            .sortedByDescending { it.lastModified() }

        val result = infos.mapNotNull { f ->
            runCatching { AppJson.decodeFromString<ItemsX>(f.readText()) }
                .onFailure { Timber.e(it, "X saved: битый .info ${f.absolutePath}") }
                .getOrNull()
        }
        _downloadedVideoIds.value = videoIds
        _downloadedPosterIds.value = posterIds
        _list.value = result
    }
}
