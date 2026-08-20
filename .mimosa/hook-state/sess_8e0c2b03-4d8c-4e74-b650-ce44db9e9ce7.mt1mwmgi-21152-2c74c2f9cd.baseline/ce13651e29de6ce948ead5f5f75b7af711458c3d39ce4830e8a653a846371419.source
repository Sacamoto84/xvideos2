package com.client.xvideos.l.featured.share

import com.client.xvideos.common.AppPath
import com.client.xvideos.l.featured.saved.lCreateMediaClient
import com.client.xvideos.l.featured.saved.lDownloadToFile
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lSavedFileName
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
    val url = item.lDownloadUrl() ?: return null
    val file = File(AppPath.l_cacheDownload, fileName)

    val client = lCreateMediaClient()

    client.use { client ->
        lDownloadToFile(client, url, file)
    }

    return file.takeIf { it.exists() && it.length() > 0L }
}
