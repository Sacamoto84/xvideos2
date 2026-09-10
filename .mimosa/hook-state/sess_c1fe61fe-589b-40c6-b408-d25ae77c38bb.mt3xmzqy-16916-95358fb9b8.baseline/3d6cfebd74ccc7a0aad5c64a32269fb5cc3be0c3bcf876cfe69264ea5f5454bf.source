package com.client.xvideos.r.common.downloader

import com.client.xvideos.common.AppPath
import com.google.gson.GsonBuilder
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.GifsInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
) {

    //Процент скачивания 0..1 - начало скачивания, -2 busy, -3 error
    var percent = MutableStateFlow(-2f)

    @OptIn(DelicateCoroutinesApi::class)
    fun downloadRedName(item: GifsInfo, onComplete: () -> Unit = {}) {

        val videoUrl = item.downloadVideoUrl()
        if ((videoUrl == null) || (item.userName == "")) {
            //Toast("Ошибка в названии файла или креатор")
            percent.value = -3f
            return
        }

        percent.value = -2f

        //Проверка того что в кеше есть запись с этим именем и кретором

        //Записи нет можно скачивать
        if (!findVideoInDownload(item.id, item.userName)) {

            val p = AppPath.r_cache_download + "/" + item.userName
            File(p).mkdirs()

            item.previewUrl()?.let { imageUrl ->
                val requestImage = kDownloader.newRequestBuilder(imageUrl, p, "${item.id}.jpg").build()
                kDownloader.enqueue(requestImage)
            }

            val request = kDownloader.newRequestBuilder(videoUrl, p, "${item.id}.mp4").tag(item.id).build()

            kDownloader.enqueue(
                request,
                onStart = {
                    println("!!! Запуск закачки")
                    percent.value = 0f
                },

                onError = {
                    println("!!! onError закачки: $it"); percent.value = -3f
                    SnackBar.error("Ошибка закачки: $it")
                },

                onProgress = { it1 -> percent.value = it1 / 100f },
                onCompleted = {
                    println("!!! onCompleted закачки")
                    percent.value = -2f

                    SnackBar.success("Скачивание завершено")
                    val gson = GsonBuilder().create()
                    val text = gson.toJson(item)
                    File(p, "${item.id}.info").writeText(text.toString())

                    onComplete()
                    //DownloadRed.refreshDownloadList()
                },
            )
        } else {
            //Toast("Файл есть к кеше")
            SnackBar.info("Файл есть к кеше")
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

        val p = AppPath.r_cache_download + "/" + item.userName
        File(p).mkdirs()

        val videoFile = File(p, "${item.id}.mp4")
        val previewFile = File(p, "${item.id}.jpg")
        var queuedVideo = 0
        var queuedPreview = 0
        var skippedNoVideoUrl = 0
        var skippedNoPreviewUrl = 0

        if (!previewFile.exists()) {
            val previewUrl = item.previewUrl()
            if (previewUrl == null) {
                skippedNoPreviewUrl++
            } else {
                val requestImage = kDownloader.newRequestBuilder(previewUrl, p, "${item.id}.jpg").build()
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

        if (!videoFile.exists()) {
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
                        onComplete()
                    }
                )
                queuedVideo++
            }
        }

        if (queuedVideo == 0) onComplete()

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
        if (item.id.isBlank() || item.userName.isBlank()) {
            return RedDownloadEnqueueReport()
        }

        val folderPath = AppPath.r_cache_download + "/" + item.userName
        File(folderPath).mkdirs()

        val videoFile = File(folderPath, "${item.id}.mp4")
        val previewFile = File(folderPath, "${item.id}.jpg")
        var queuedVideo = 0
        var queuedPreview = 0
        var skippedNoVideoUrl = 0
        var skippedNoPreviewUrl = 0

        if (!previewFile.exists()) {
            val previewUrl = item.previewUrl()
            if (previewUrl == null) {
                skippedNoPreviewUrl++
                onEvent("R Download: нет preview URL ${item.id}")
            } else {
                val requestImage = kDownloader.newRequestBuilder(previewUrl, folderPath, "${item.id}.jpg").build()
                kDownloader.enqueue(
                    requestImage,
                    onStart = { onEvent("R Download: старт preview ${item.id}") },
                    onError = { error -> onEvent("R Download: preview не скачан ${item.id}: $error") },
                    onCompleted = { onEvent("R Download: preview готов ${item.id}") }
                )
                queuedPreview++
            }
        }

        if (!videoFile.exists()) {
            val videoUrl = item.downloadVideoUrl()
            if (videoUrl == null) {
                skippedNoVideoUrl++
                onEvent("R Download: нет video URL ${item.id}")
            } else {
                val request = kDownloader.newRequestBuilder(videoUrl, folderPath, "${item.id}.mp4").tag(item.id).build()
                kDownloader.enqueue(
                    request,
                    onStart = {
                        percent.value = 0f
                        onEvent("R Download: старт video ${item.id}")
                    },
                    onError = { error ->
                        percent.value = -3f
                        onEvent("R Download: video не скачан ${item.id}: $error")
                    },
                    onProgress = { progress -> percent.value = progress / 100f },
                    onCompleted = {
                        percent.value = -2f
                        onEvent("R Download: video готов ${item.id}")
                        onComplete()
                    }
                )
                queuedVideo++
            }
        }

        if (queuedVideo == 0) onComplete()

        return RedDownloadEnqueueReport(
            queuedVideo = queuedVideo,
            queuedPreview = queuedPreview,
            skippedNoVideoUrl = skippedNoVideoUrl,
            skippedNoPreviewUrl = skippedNoPreviewUrl
        )
    }

    fun findVideoInDownload(id: String, name: String): Boolean {
        //val mainPath = AppPath.cache_download_red + "/" + name + "/" + id + ".mp4"
        val mainPath = "${AppPath.r_cache_download}/$name/$id.mp4"
        val file = File(mainPath)
        return file.exists()
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
