package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.r.model.GifsInfo
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.File

/**
 * Импорт R-бандла: «лайк» в R — запись [GifsInfo] в FileDB, физические файлы
 * не раскладываются (LikesTab рендерит по URL из метаданных).
 * Парсит `.info` из бандла и отдаёт в [addLike] (вызывающий подставляет
 * `savedRed.likes::add`).
 */
class RLikesBundleImporter(
    private val addLike: (GifsInfo) -> Unit,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val meta = manifest.files.firstOrNull { it.name == manifest.metadataFileName }
            ?: error("В манифесте нет метаданных (${manifest.metadataFileName})")
        val file = receivedFiles[meta.payloadId]
            ?: error("Файл метаданных не получен (payloadId=${meta.payloadId})")
        val item = GsonBuilder().create().fromJson(file.readText(), GifsInfo::class.java)
            ?: error("Пустые метаданные")
        Timber.d("P2P R: импорт лайка id=${item.id} user=${item.userName}")
        addLike(item)
    }
}
