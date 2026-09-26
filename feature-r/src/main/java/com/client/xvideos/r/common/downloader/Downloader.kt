package com.client.xvideos.r.common.downloader

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.GifsInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import timber.log.Timber
import androidx.compose.runtime.Immutable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Описание элемента готового загруженного кэша RedGifs.
 *
 * @property name Имя автора (соответствует имени подпапки).
 * @property id Уникальный идентификатор ролика (соответствует имени файла).
 * @property url URL источника скачивания.
 */
@Immutable
data class ItemsRedDownload(
    val name: String = "",     //Название креатора соответствует папке
    val id: String,            //Имя файла уникально
    val url: String = "",      //Создается на этапе закачки, и после успешной закачки не используется url mp4  //https://media.redgifs.com/VictoriousGlamorousStud.m4s
)

/**
 * Отчет о постановке недостающих файлов (видео и превью) в очередь загрузчика.
 *
 * @property queuedVideo Количество видеофайлов (.mp4), поставленных в очередь.
 * @property queuedPreview Количество изображений превью (.jpg), поставленных в очередь.
 * @property skippedNoVideoUrl Пропущено из-за отсутствия валидной ссылки на видео.
 * @property skippedNoPreviewUrl Пропущено из-за отсутствия валидной ссылки на превью.
 */
@Immutable
data class RedDownloadEnqueueReport(
    val queuedVideo: Int = 0,
    val queuedPreview: Int = 0,
    val skippedNoVideoUrl: Int = 0,
    val skippedNoPreviewUrl: Int = 0
) {
    /** Суммарное количество файлов, поставленных в очередь загрузки. */
    val totalQueued: Int get() = queuedVideo + queuedPreview

    /** Суммарное количество пропущенных файлов. */
    val totalSkipped: Int get() = skippedNoVideoUrl + skippedNoPreviewUrl

    /** Флаг наличия хотя бы одного файла, поставленного в очередь. */
    val hasQueued: Boolean get() = totalQueued > 0

    /** Флаг отсутствия пропущенных файлов при постановке в очередь. */
    val isClean: Boolean get() = totalSkipped == 0
}

/**
 * Низкоуровневый сервис скачивания медиафайлов RedGifs на базе [KDownloader].
 *
 * Сохраняет видео (`.mp4`), превью (`.jpg`) и метаданные (`.info`) в каталог `AppPath.r_cache_download/<userName>/`.
 *
 * @property kDownloader Движок многопоточного скачивания файлов.
 * @param scope Корутин-скоп для выполнения дисковых операций и колбэков.
 */
@Singleton
class Downloader @Inject constructor(
    val kDownloader: KDownloader,
    @ApplicationScope private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    /**
     * Прогресс текущей загрузки:
     * - `0.0..1.0` — активный прогресс скачивания;
     * - `-2f` — состояние простоя (idle/готов);
     * - `-3f` — ошибка скачивания.
     */
    val percent = MutableStateFlow(-2f)

    /** Проверяет, идет ли активный процесс скачивания. */
    fun isDownloading(): Boolean = percent.value in 0f..1f

    /** Проверяет, находится ли загрузчик в состоянии покоя. */
    fun isIdle(): Boolean = percent.value == -2f

    /** Проверяет, завершилась ли последняя загрузка ошибкой. */
    fun hasDownloadError(): Boolean = percent.value == -3f

    /**
     * Скачивает медиафайл [item] (видео и превью), если он еще не присутствует на диске.
     * По завершении атомарно создает файл метаданных `<id>.info` и вызывает [onComplete].
     *
     * @param item Загружаемый медиаэлемент.
     * @param onComplete Коллбэк завершения загрузки.
     */
    fun downloadRedName(item: GifsInfo, onComplete: () -> Unit = {}) {

        val videoUrl = item.downloadVideoUrl()
        if (videoUrl == null || item.userName.isBlank() || item.id.isBlank()) {
            //Toast("Ошибка в названии файла или креатор")
            percent.value = -3f
            return
        }

        if (isUnsafeItemName(item.userName) || isUnsafeItemName(item.id)) {
            percent.value = -3f
            SnackBar.error("Недопустимое имя файла или креатора")
            return
        }

        val rootDir = File(AppPath.r_cache_download)
        val creatorDir = File(rootDir, item.userName)
        try {
            requireInside(rootDir, creatorDir)
        } catch (e: Exception) {
            Timber.w(e, "Downloader: недопустимый путь к папке креатора: ${item.userName}")
            percent.value = -3f
            SnackBar.error("Недопустимый путь к папке креатора")
            return
        }

        percent.value = -2f

        //Проверка того что в кеше есть запись с этим именем и кретором

        //Записи нет можно скачивать
        if (!findVideoInDownload(item.id, item.userName)) {

            val creatorPath = creatorDir.absolutePath
            creatorDir.mkdirs()

            val previewFile = File(creatorPath, "${item.id}.jpg")
            if (!previewFile.exists() || previewFile.length() == 0L) {
                enqueuePreview(item, creatorPath, showSnackBarErrors = false, onEvent = {})
            }

            val request = kDownloader.newRequestBuilder(videoUrl, creatorPath, "${item.id}.mp4").tag(item.id).build()

            kDownloader.enqueue(
                request,
                onStart = {
                    Timber.i("Downloader: запуск закачки id=${item.id}")
                    percent.value = 0f
                },

                onError = {
                    Timber.e("Downloader: ошибка закачки id=${item.id}: $it")
                    percent.value = -3f
                    SnackBar.error("Ошибка закачки: $it")
                },

                onProgress = { it1 -> percent.value = it1 / 100f },
                onCompleted = {
                    Timber.i("Downloader: завершено скачивание id=${item.id}")
                    scope.launch(Dispatchers.IO) {
                        val videoFile = File(creatorPath, "${item.id}.mp4")
                        if (!videoFile.exists() || videoFile.length() == 0L) {
                            videoFile.delete()
                            percent.value = -3f
                            withContext(Dispatchers.Main) {
                                SnackBar.error("Ошибка: скачанный файл пуст")
                            }
                            return@launch
                        }
                        percent.value = -2f
                        withContext(Dispatchers.Main) {
                            SnackBar.success("Скачивание завершено")
                        }
                        runCatching {
                            val text = AppJson.encodeToString(item)
                            File(creatorPath, "${item.id}.info").writeTextAtomically(text)
                        }.onFailure {
                            Timber.e(it, "Downloader: ошибка записи .info для ${item.id}")
                        }
                        withContext(Dispatchers.Main) {
                            onComplete()
                        }
                    }
                },
            )
        } else {
            SnackBar.info("Файл есть в кеше")
            scope.launch(Dispatchers.Main) {
                onComplete()
            }
        }

    }

    /**
     * Проверяет наличие видео и превью для [item], и докачивает только недостающие файлы.
     *
     * @param item Медиаэлемент.
     * @param onComplete Коллбэк после завершения скачивания всех недостающих файлов.
     * @param onEvent Лог-коллбэк событий.
     * @param showSnackBarErrors Показывать ли сообщения об ошибках в снэкбаре.
     * @return [RedDownloadEnqueueReport] со статистикой постановки в очередь.
     */
    fun downloadMissingFiles(
        item: GifsInfo,
        onComplete: () -> Unit = {},
        onEvent: (String) -> Unit = {},
        showSnackBarErrors: Boolean = true
    ): RedDownloadEnqueueReport {
        if (item.id.isBlank() || item.userName.isBlank()) {
            return RedDownloadEnqueueReport()
        }
        if (isUnsafeItemName(item.userName) || isUnsafeItemName(item.id)) {
            return RedDownloadEnqueueReport()
        }

        val rootDir = File(AppPath.r_cache_download)
        val creatorDir = File(rootDir, item.userName)
        try {
            requireInside(rootDir, creatorDir)
        } catch (e: Exception) {
            Timber.w(e, "Downloader: недопустимый путь к папке креатора: ${item.userName}")
            return RedDownloadEnqueueReport()
        }

        val creatorPath = creatorDir.absolutePath
        creatorDir.mkdirs()

        val videoFile = File(creatorPath, "${item.id}.mp4")
        val previewFile = File(creatorPath, "${item.id}.jpg")
        var queuedVideo = 0
        var queuedPreview = 0
        var skippedNoVideoUrl = 0
        var skippedNoPreviewUrl = 0

        if (!previewFile.exists() || previewFile.length() == 0L) {
            if (item.previewUrl() == null) {
                skippedNoPreviewUrl++
            } else {
                enqueuePreview(item, creatorPath, showSnackBarErrors, onEvent)
                queuedPreview++
            }
        }

        if (!videoFile.exists() || videoFile.length() == 0L) {
            if (item.downloadVideoUrl() == null) {
                skippedNoVideoUrl++
            } else {
                enqueueVideo(item, creatorPath, videoFile, showSnackBarErrors, onEvent, onComplete)
                queuedVideo++
            }
        }

        if (queuedVideo == 0) {
            scope.launch(Dispatchers.Main) {
                onComplete()
            }
        }

        return RedDownloadEnqueueReport(
            queuedVideo = queuedVideo,
            queuedPreview = queuedPreview,
            skippedNoVideoUrl = skippedNoVideoUrl,
            skippedNoPreviewUrl = skippedNoPreviewUrl
        )
    }

    /**
     * Вариант [downloadMissingFiles] для фонового восстановления поврежденных или незавершенных загрузок (без всплывающих снэкбаров).
     */
    fun downloadMissingFilesForRecovery(
        item: GifsInfo,
        onComplete: () -> Unit = {},
        onEvent: (String) -> Unit = {}
    ): RedDownloadEnqueueReport {
        return downloadMissingFiles(
            item = item,
            onComplete = onComplete,
            onEvent = onEvent,
            showSnackBarErrors = false
        )
    }

    /**
     * Быстрая проверка наличия непустого `.mp4` файла в локальной папке загрузок `<r_cache_download>/<name>/<id>.mp4`.
     *
     * @param id Идентификатор медиафайла.
     * @param name Имя автора.
     * @return true, если файл существует и имеет размер более 0 байт.
     */
    fun findVideoInDownload(id: String, name: String): Boolean {
        if (id.isBlank() || name.isBlank()) return false
        if (isUnsafeItemName(name) || isUnsafeItemName(id)) return false
        val baseDir = File(AppPath.r_cache_download)
        val userDir = File(baseDir, name)
        val file = File(userDir, "$id.mp4")
        return try {
            requireInside(baseDir, userDir)
            requireInside(userDir, file)
            file.exists() && file.length() > 0L
        } catch (e: Exception) {
            Timber.w(e, "Downloader.findVideoInDownload -> Попытка выхода за пределы r_cache_download")
            false
        }
    }

    /**
     * Ставит в очередь загрузчика скачивание картинки превью (`.jpg`).
     */
    private fun enqueuePreview(
        item: GifsInfo,
        dirPath: String,
        showSnackBarErrors: Boolean,
        onEvent: (String) -> Unit
    ) {
        val previewUrl = item.previewUrl() ?: return
        val requestImage = kDownloader.newRequestBuilder(previewUrl, dirPath, "${item.id}.jpg").tag(item.id).build()
        kDownloader.enqueue(
            requestImage,
            onStart = { onEvent("R Download: старт preview ${item.id}") },
            onError = { error ->
                onEvent("R Download: preview не скачан ${item.id}: $error")
                if (showSnackBarErrors) {
                    SnackBar.error("Ошибка загрузки preview: $error")
                }
            },
            onCompleted = { onEvent("R Download: preview готов ${item.id}") }
        )
    }

    /**
     * Ставит в очередь загрузчика скачивание основного видеофайла (`.mp4`).
     */
    private fun enqueueVideo(
        item: GifsInfo,
        dirPath: String,
        videoFile: File,
        showSnackBarErrors: Boolean,
        onEvent: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val videoUrl = item.downloadVideoUrl() ?: return
        val request = kDownloader.newRequestBuilder(videoUrl, dirPath, "${item.id}.mp4").tag(item.id).build()
        kDownloader.enqueue(
            request,
            onStart = { percent.value = 0f },
            onError = {
                percent.value = -3f
                onEvent("R Download: video не скачан ${item.id}: $it")
                if (showSnackBarErrors) {
                    SnackBar.error("Ошибка закачки: $it")
                }
            },
            onProgress = { progress -> percent.value = progress / 100f },
            onCompleted = {
                scope.launch(Dispatchers.IO) {
                    if (!videoFile.exists() || videoFile.length() == 0L) {
                        videoFile.delete()
                        percent.value = -3f
                        onEvent("R Download: video пустой или повреждён ${item.id}")
                        if (showSnackBarErrors) {
                            withContext(Dispatchers.Main) {
                                SnackBar.error("Ошибка: скачанный файл пуст")
                            }
                        }
                        return@launch
                    }
                    percent.value = -2f
                    onEvent("R Download: video готов ${item.id}")
                    val infoFile = File(dirPath, "${item.id}.info")
                    if (!infoFile.exists() || infoFile.length() == 0L) {
                        runCatching {
                            infoFile.writeTextAtomically(AppJson.encodeToString(item))
                        }.onFailure {
                            Timber.e(it, "Downloader: ошибка записи .info для ${item.id}")
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                }
            }
        )
    }
}

/**
 * Выбирает наиболее качественный доступный URL для скачивания видео (HD -> SD -> Silent).
 */
internal fun GifsInfo.downloadVideoUrl(): String? {
    return urls.hd?.takeIf { it.isNotBlank() }
        ?: urls.sd.takeIf { it.isNotBlank() }
        ?: urls.silent?.takeIf { it.isNotBlank() }
}

/**
 * Выбирает лучший URL для превью (Poster -> Thumbnail).
 */
internal fun GifsInfo.previewUrl(): String? {
    return urls.poster?.takeIf { it.isNotBlank() }
        ?: urls.thumbnail.takeIf { it.isNotBlank() }
}
