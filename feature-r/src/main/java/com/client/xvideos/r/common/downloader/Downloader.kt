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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

//Текущее содержимое готового кеша
data class ItemsRedDownload(
    val name: String = "",     //Название креатора соответствует папке
    val id: String,            //Имя файла уникально
    val url: String = "",      //Создается на этапе закачки, и после успешной закачки не используется url mp4  //https://media.redgifs.com/VictoriousGlamorousStud.m4s
)

data class RedDownloadEnqueueReport(
    val queuedVideo: Int = 0,
    val queuedPreview: Int = 0,
    val skippedNoVideoUrl: Int = 0,
    val skippedNoPreviewUrl: Int = 0
)

/**
 * Проверка что данное имя креатор уже есть в кеше
 */
@Singleton
class Downloader @Inject constructor(
    val kDownloader: KDownloader,
    @ApplicationScope private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    //Процент скачивания 0..1 - начало скачивания, -2 busy, -3 error
    var percent = MutableStateFlow(-2f)

    fun downloadRedName(item: GifsInfo, onComplete: () -> Unit = {}) {

        val videoUrl = item.downloadVideoUrl()
        if ((videoUrl == null) || (item.userName == "") || (item.id == "")) {
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

            val p = creatorDir.absolutePath
            creatorDir.mkdirs()

            item.previewUrl()?.let { imageUrl ->
                val requestImage = kDownloader.newRequestBuilder(imageUrl, p, "${item.id}.jpg").tag(item.id).build()
                kDownloader.enqueue(requestImage)
            }

            val request = kDownloader.newRequestBuilder(videoUrl, p, "${item.id}.mp4").tag(item.id).build()

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
                    percent.value = -2f

                    SnackBar.success("Скачивание завершено")
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            val text = AppJson.encodeToString(item)
                            File(p, "${item.id}.info").writeTextAtomically(text)
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

        val p = creatorDir.absolutePath
        creatorDir.mkdirs()

        val videoFile = File(p, "${item.id}.mp4")
        val previewFile = File(p, "${item.id}.jpg")
        var queuedVideo = 0
        var queuedPreview = 0
        var skippedNoVideoUrl = 0
        var skippedNoPreviewUrl = 0

        if (!previewFile.exists() || previewFile.length() == 0L) {
            val previewUrl = item.previewUrl()
            if (previewUrl == null) {
                skippedNoPreviewUrl++
            } else {
                val requestImage = kDownloader.newRequestBuilder(previewUrl, p, "${item.id}.jpg").tag(item.id).build()
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
                queuedPreview++
            }
        }

        if (!videoFile.exists() || videoFile.length() == 0L) {
            val videoUrl = item.downloadVideoUrl()
            if (videoUrl == null) {
                skippedNoVideoUrl++
            } else {
                val request = kDownloader.newRequestBuilder(videoUrl, p, "${item.id}.mp4").tag(item.id).build()
                kDownloader.enqueue(
                    request,
                    onStart = {
                        percent.value = 0f
                    },
                    onError = {
                        percent.value = -3f
                        onEvent("R Download: video не скачан ${item.id}: $it")
                        SnackBar.error("Ошибка закачки: $it")
                    },
                    onProgress = { progress -> percent.value = progress / 100f },
                    onCompleted = {
                        percent.value = -2f
                        onEvent("R Download: video готов ${item.id}")
                        scope.launch(Dispatchers.IO) {
                            val infoFile = File(p, "${item.id}.info")
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

}

internal fun GifsInfo.downloadVideoUrl(): String? {
    return urls.hd?.takeIf { it.isNotBlank() }
        ?: urls.sd.takeIf { it.isNotBlank() }
        ?: urls.silent?.takeIf { it.isNotBlank() }
}

internal fun GifsInfo.previewUrl(): String? {
    return urls.poster?.takeIf { it.isNotBlank() }
        ?: urls.thumbnail.takeIf { it.isNotBlank() }
}
