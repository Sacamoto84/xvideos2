package com.client.xvideos.common.p2p

import java.io.File

/** Раскладывает принятые файлы по [P2pManifestFile.relativePath] внутри storeRoot (перезапись). */
object P2pBundleInstaller {

    fun install(
        storeRoot: File,
        manifest: P2pManifest,
        receivedFiles: Map<Long, File>,
    ): List<File> {
        return manifest.files.map { entry ->
            val source = receivedFiles[entry.payloadId]
                ?: error("Missing received file for payloadId ${entry.payloadId} (${entry.name})")
            val target = File(storeRoot, entry.relativePath)
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
            target
        }
    }
}
