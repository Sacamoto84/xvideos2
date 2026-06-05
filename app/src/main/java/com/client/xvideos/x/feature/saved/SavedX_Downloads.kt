package com.client.xvideos.x.feature.saved

import android.net.Uri
import com.client.xvideos.App
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.urlStart
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parseHTML5Player
import com.client.xvideos.x.parcer.parserItemVideo
import com.google.gson.GsonBuilder
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
    private val kDownloader by lazy { KDownloader.create(App.instance.applicationContext) }
    private val gson = GsonBuilder().create()

    /** `0f..1f` — прогресс, `-2f` — простой/готово, `-3f` — ошибка. */
    val percent = MutableStateFlow(-2f)

    private val _list = MutableStateFlow<List<ItemsX>>(emptyList())
    val list: StateFlow<List<ItemsX>> = _list.asStateFlow()

    init {
        refresh()
    }

    /** O(1)-ish проверка: файл видео уже сохранён. */
    fun contains(id: Long): Boolean = File(dir, "$id.mp4").exists()

    /** `file://`-URI скачанного видео (для ExoPlayer). */
    fun localUrl(id: Long): String = Uri.fromFile(File(dir, "$id.mp4")).toString()

    /**
     * Абсолютный путь к локальной картинке превью (`<id>.jpg`), если она скачана, иначе null.
     * UrlImage сам грузит локальный файл, если строка не начинается с `https://`.
     */
    fun localPosterPath(id: Long): String? {
        val f = File(dir, "$id.jpg")
        return if (f.exists()) f.absolutePath else null
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
        if (contains(item.id)) {
            SnackBar.info("Уже сохранено")
            return
        }

        percent.value = -2f
        SnackBar.info("Получение ссылки на видео…")

        scope.launch(Dispatchers.IO) {
            // Резолвим прямой mp4 (лучшее качество) со страницы видео.
            val videoUrl = runCatching {
                val pageUrl = if (item.href.startsWith("http")) item.href else urlStart + item.href
                val html = readHtmlFromURLDirect(pageUrl)
                val config = parserItemVideo(html)?.let { parseHTML5Player(it) }
                config?.videoUrlHigh?.takeIf { it.isNotBlank() }
                    ?: config?.videoUrlLow?.takeIf { it.isNotBlank() }
            }.getOrNull()

            if (videoUrl.isNullOrBlank()) {
                percent.value = -3f
                SnackBar.error("Не удалось получить ссылку на видео")
                return@launch
            }

            File(dir).mkdirs()

            // Превью-картинка (необязательно — ошибки не критичны).
            if (item.previewImage.isNotBlank()) {
                runCatching {
                    val reqImg = kDownloader.newRequestBuilder(item.previewImage, dir, "${item.id}.jpg").build()
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
                    percent.value = -2f
                    runCatching {
                        File(dir, "${item.id}.info").writeText(gson.toJson(item))
                    }.onFailure { Timber.e(it, "X download: ошибка записи .info ${item.id}") }
                    SnackBar.success("Скачано")
                    refresh()
                },
            )
        }
    }

    fun delete(item: ItemsX) {
        scope.launch(Dispatchers.IO) {
            File(dir, "${item.id}.mp4").delete()
            File(dir, "${item.id}.jpg").delete()
            File(dir, "${item.id}.info").delete()
            refresh()
            SnackBar.info("Удалено из сохранённого")
        }
    }

    /** Перечитать список сохранённого по `.info`-файлам. */
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val root = File(dir)
            val infos = if (root.exists() && root.isDirectory) {
                root.listFiles { f -> f.isFile && f.extension == "info" }?.toList() ?: emptyList()
            } else {
                emptyList()
            }

            val result = infos.mapNotNull { f ->
                runCatching { gson.fromJson(f.readText(), ItemsX::class.java) }
                    .onFailure { Timber.e(it, "X saved: битый .info ${f.absolutePath}") }
                    .getOrNull()
            }
            _list.value = result
        }
    }
}
