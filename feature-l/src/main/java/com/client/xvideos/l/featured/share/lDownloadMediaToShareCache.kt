package com.client.xvideos.l.featured.share

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.io.isUnsafeItemName
import com.client.xvideos.common.io.requireInside
import com.client.xvideos.l.featured.saved.lCreateMediaClient
import com.client.xvideos.l.featured.saved.lDownloadToFile
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lSavedFileName
import timber.log.Timber
import java.io.File

/**
 * Скачивает медиа элемента [item] в share-кеш и возвращает локальный файл.
 *
 * Качает потоково ([lDownloadToFile]) с корректными media-заголовками, поэтому
 * безопасно по памяти (большие видео не буферизуются целиком в RAM) и должна
 * вызываться на IO-диспетчере. Возвращает `null`, если у элемента нет ссылки
 * на скачивание.
 *
 * @throws Exception при сетевой/файловой ошибке скачивания.
 */
suspend fun lDownloadMediaToShareCache(item: PicsDetails): File? {
    val fileName = item.lSavedFileName() ?: return null
    if (isUnsafeItemName(fileName)) return null

    val rootDir = File(AppPath.l_cacheDownload)
    val file = File(rootDir, fileName)
    try {
        requireInside(rootDir, file)
    } catch (e: Exception) {
        Timber.w(e, "lDownloadMediaToShareCache -> Попытка выхода за пределы l_cacheDownload")
        return null
    }

    val url = item.lDownloadUrl() ?: return null
    val client = lCreateMediaClient()

    client.use { client ->
        lDownloadToFile(client, url, file)
    }

    return file.takeIf { it.exists() && it.length() > 0L }
}
