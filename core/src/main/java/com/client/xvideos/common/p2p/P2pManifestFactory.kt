package com.client.xvideos.common.p2p

import com.client.xvideos.common.io.requireInside
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
        val canonicalRoot = storeRoot.canonicalFile
        val entries = files.map { file ->
            val canonicalFile = file.canonicalFile
            requireInside(canonicalRoot, canonicalFile)
            val rel = canonicalFile.relativeTo(canonicalRoot).invariantSeparatorsPath
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
