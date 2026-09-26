package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.l.model.AlbumDetails
import kotlinx.serialization.encodeToString
import java.io.File

/**
 * Альбом L — только файл метаданных `<id>.album` (контент получатель качает сам).
 * Сохранённый альбом берётся из [savedRoot] (`AppPath.l_albums`); несохранённый
 * сериализуется в [outboxAlbumRoot] (outbox-зеркало l_albums) в формате FileDB
 * (JSON, pretty printing). Возвращает null при невалидном id или ошибке записи.
 *
 * Лежит в разделе, а не рядом с остальными экспортёрами: он единственный из
 * них знает модель раздела — [AlbumDetails].
 */
object LAlbumExporter {

    fun getAlbumFileName(albumId: String): String = "$albumId.album"
    fun getAlbumFileName(album: AlbumDetails): String = getAlbumFileName(album.id)

    fun isAlbumSaved(albumId: String, savedRoot: File): Boolean {
        if (albumId.toLongOrNull() == null) return false
        val file = File(savedRoot, getAlbumFileName(albumId))
        return file.exists() && file.length() > 0L
    }

    /**
     * Экспортирует метаданные альбома [album] для отправки по P2P.
     *
     * @param album Детали экспортируемого альбома.
     * @param savedRoot Корневая директория уже сохраненных альбомов (`AppPath.l_albums`).
     * @param outboxAlbumRoot Директория временных файлов отправки.
     * @return [P2pExportBundle] с типом [P2pType.L_ALBUM] и файлом метаданных, либо `null` при ошибке.
     */
    fun export(album: AlbumDetails, savedRoot: File, outboxAlbumRoot: File): P2pExportBundle? {
        if (album.id.toLongOrNull() == null) return null
        val fileName = getAlbumFileName(album)

        val savedFile = File(savedRoot, fileName)
        if (savedFile.exists() && savedFile.length() > 0L) {
            return P2pExportBundle(P2pType.L_ALBUM, savedRoot, listOf(savedFile), savedFile)
        }

        return runCatching {
            outboxAlbumRoot.mkdirs()
            val outFile = File(outboxAlbumRoot, fileName)
            outFile.writeTextAtomically(AppJson.encodeToString(album))
            P2pExportBundle(P2pType.L_ALBUM, outboxAlbumRoot, listOf(outFile), outFile)
        }.getOrNull()
    }
}
