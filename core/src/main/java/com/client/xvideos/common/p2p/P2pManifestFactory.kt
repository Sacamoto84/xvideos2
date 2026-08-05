package com.client.xvideos.common.p2p

import java.io.File

/** Строит [P2pManifest] из файлов бандла: relativePath считается от [P2pExportBundle.storeRoot]. */
object P2pManifestFactory {

    fun create(
        type: P2pType,
        storeRoot: File,
        files: List<File>,
        metadataFile: File?,
        payloadIds: Map<File, Long>,
    ): P2pManifest {
        val rootPath = storeRoot.absoluteFile.normalize().path
        val entries = files.map { file ->
            val abs = file.absoluteFile.normalize().path
            require(abs.startsWith(rootPath)) { "File $abs is not inside store root $rootPath" }
            val rel = abs.removePrefix(rootPath)
                .trimStart(File.separatorChar)
                .replace(File.separatorChar, '/')
            val payloadId = payloadIds[file] ?: error("Missing payloadId for ${file.name}")
            P2pManifestFile(
                name = file.name,
                relativePath = rel,
                payloadId = payloadId,
                size = file.length(),
            )
        }
        return P2pManifest(type = type, metadataFileName = metadataFile?.name, files = entries)
    }
}
