package com.client.xvideos.x.feature.saved

import com.client.xvideos.App
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.x.model.ItemsX
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
 * X-видео отдаётся по HLS и не качается одним файлом, поэтому здесь, как и в R,
 * скачивается прямой mp4 — короткое превью [ItemsX.previewVideo] — плюс картинка
 * превью и `.info`-json с метаданными. Файлы складываются в [AppPath.x_cache_download].
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

    /**
     * Скачать видео (превью-mp4) в [AppPath.x_cache_download].
     * Прогресс отражается в [percent], по завершении пишется `.info` и шлётся снекбар.
     */
    fun download(item: ItemsX) {
        val videoUrl = item.previewVideo
        if (videoUrl.isBlank()) {
            percent.value = -3f
            SnackBar.error("Нет ссылки на видео для скачивания")
            return
        }

        if (contains(item.id)) {
            SnackBar.info("Уже сохранено")
            return
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
