package com.client.xvideos.r.common.downloader

import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pType
import java.io.File

/**
 * Чистая сборка metadata-бандла для P2P (R): только `.info` (+ `.jpg`, если уже лежит
 * рядом в `<tmpRoot>/<userName>/`). Видео не участвует — получатель стримит по URL
 * из метаданных. Сетевой части нет — тестируемо.
 *
 * @param tmpRoot корень временной папки экспорта; станет storeRoot бандла,
 *   relativePath файлов получится `<userName>/<id>.*` — получатель положит их
 *   в `r_cache_download` с той же структурой.
 */
fun buildRMetaBundle(tmpRoot: File, userName: String, id: String, infoJson: String): P2pExportBundle {
    val dir = File(tmpRoot, userName).apply { mkdirs() }
    val info = File(dir, "$id.info").apply { writeText(infoJson) }
    val jpg = File(dir, "$id.jpg")
    val files = buildList {
        add(info)
        if (jpg.exists()) add(jpg)
    }
    return P2pExportBundle(P2pType.R, tmpRoot, files, info)
}
