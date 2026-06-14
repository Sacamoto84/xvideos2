package com.client.xvideos.common.p2p.export

import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.zip.ZipUtils
import com.client.xvideos.l.model.AlbumDetails
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Exporters берут уже сохранённый/скачанный item и собирают [P2pExportBundle].
 * Корни store передаются параметром (тестируемость); вызывающий подставляет `AppPath.*`.
 * Возвращают null, если файлов на диске нет (v1: «Сначала сохрани»).
 */
object XExporter {
    fun export(storeRoot: File, id: Long): P2pExportBundle? {
        val l = XBundleLocator.locate(storeRoot, id) ?: return null
        return P2pExportBundle(P2pType.X, l.storeRoot, l.files, l.metadataFile)
    }
}

object RExporter {
    fun export(storeRoot: File, userName: String, id: String): P2pExportBundle? {
        val l = RBundleLocator.locate(storeRoot, userName, id) ?: return null
        return P2pExportBundle(P2pType.R, l.storeRoot, l.files, l.metadataFile)
    }
}

object LExporter {
    /** @param itemFolder папка сохранённого L-item (вызывающий находит её через существующий `lFindLikeFolder`). */
    fun export(itemFolder: File): P2pExportBundle? {
        val l = LBundleLocator.locate(itemFolder) ?: return null
        return P2pExportBundle(P2pType.L, l.storeRoot, l.files, l.metadataFile)
    }
}

/**
 * Альбом L — только файл метаданных `<id>.album` (контент получатель качает сам).
 * Сохранённый альбом берётся из [savedRoot] (`AppPath.l_albums`); несохранённый
 * сериализуется в [outboxAlbumRoot] (outbox-зеркало l_albums) в формате FileDB
 * (Gson, pretty printing). Возвращает null при невалидном id или ошибке записи.
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

/**
 * Коллекция L — вся папка коллекции одним zip-архивом (реальные файлы, не
 * только метаданные). Архив пишется в [outboxDir] как `<имя>.zip`; записи
 * внутри начинаются с имени коллекции, поэтому на приёме распаковка в зеркало
 * `inbox/L/Collection` сразу даёт `inbox/L/Collection/<имя>/...`.
 * Возвращает null, если коллекции нет или она пуста (нет ни одного файла).
 */
object LCollectionExporter {

    fun export(collectionName: String, collectionRoot: File, outboxDir: File): P2pExportBundle? {
        val source = File(collectionRoot, collectionName)
        if (!source.isDirectory) return null
        if (source.walkTopDown().none { it.isFile }) return null

        return runCatching {
            outboxDir.mkdirs()
            val zipFile = File(outboxDir, "$collectionName.zip")
            ZipUtils.zipDirectory(source, zipFile)
            P2pExportBundle(P2pType.L_COLLECTION, outboxDir, listOf(zipFile), null)
        }.getOrNull()
    }
}

/**
 * Коллекция R — папка коллекции (мелкие `.collection` JSON-ссылки) одним zip-архивом.
 * Зеркало [LCollectionExporter], отличается только типом [P2pType.R_COLLECTION].
 */
object RCollectionExporter {

    fun export(collectionName: String, collectionRoot: File, outboxDir: File): P2pExportBundle? {
        val source = File(collectionRoot, collectionName)
        if (!source.isDirectory) return null
        if (source.walkTopDown().none { it.isFile }) return null

        return runCatching {
            outboxDir.mkdirs()
            val zipFile = File(outboxDir, "$collectionName.zip")
            ZipUtils.zipDirectory(source, zipFile)
            P2pExportBundle(P2pType.R_COLLECTION, outboxDir, listOf(zipFile), null)
        }.getOrNull()
    }
}
