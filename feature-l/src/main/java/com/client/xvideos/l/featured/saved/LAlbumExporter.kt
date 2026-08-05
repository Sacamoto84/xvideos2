package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.l.model.AlbumDetails
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Альбом L — только файл метаданных `<id>.album` (контент получатель качает сам).
 * Сохранённый альбом берётся из [savedRoot] (`AppPath.l_albums`); несохранённый
 * сериализуется в [outboxAlbumRoot] (outbox-зеркало l_albums) в формате FileDB
 * (Gson, pretty printing). Возвращает null при невалидном id или ошибке записи.
 *
 * Лежит в разделе, а не рядом с остальными экспортёрами: он единственный из
 * них знает модель раздела — [AlbumDetails].
 */
object LAlbumExporter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun export(album: AlbumDetails, savedRoot: File, outboxAlbumRoot: File): P2pExportBundle? {
        if (album.id.toLongOrNull() == null) return null
        val fileName = "${album.id}.album"

        val savedFile = File(savedRoot, fileName)
        if (savedFile.exists()) {
            return P2pExportBundle(P2pType.L_ALBUM, savedRoot, listOf(savedFile), savedFile)
        }

        return runCatching {
            outboxAlbumRoot.mkdirs()
            val outFile = File(outboxAlbumRoot, fileName)
            outFile.writeText(gson.toJson(album), Charsets.UTF_8)
            P2pExportBundle(P2pType.L_ALBUM, outboxAlbumRoot, listOf(outFile), outFile)
        }.getOrNull()
    }
}
