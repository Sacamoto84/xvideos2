package com.client.xvideos.common.p2p

import com.client.xvideos.common.io.normalizeRelativePath
import com.client.xvideos.common.io.requireInside
import java.io.File

/** Раскладывает принятые файлы по [P2pManifestFile.relativePath] внутри storeRoot (перезапись). */
object P2pBundleInstaller {

    /**
     * Путь каждого файла нормализуется и проверяется на попадание внутрь
     * [storeRoot] ещё раз, хотя [P2pManifestCodec.fromJson] уже отверг бы
     * битый манифест. Это защита в глубину: установщик — последняя точка перед
     * записью на диск, и он не должен полагаться на то, что кто-то выше по
     * стеку проверил вход. Ровно на такой цепочке «проверка в другом месте»
     * дыра и держалась.
     */
    fun install(
        storeRoot: File,
        manifest: P2pManifest,
        receivedFiles: Map<Long, File>,
    ): List<File> {
        val root = storeRoot.canonicalFile
        return manifest.files.map { entry ->
            val source = receivedFiles[entry.payloadId]
                ?: error("Missing received file for payloadId ${entry.payloadId} (${entry.name})")
            val target = File(root, normalizeRelativePath(entry.relativePath)).canonicalFile
            requireInside(root, target)
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
            target
        }
    }
}
