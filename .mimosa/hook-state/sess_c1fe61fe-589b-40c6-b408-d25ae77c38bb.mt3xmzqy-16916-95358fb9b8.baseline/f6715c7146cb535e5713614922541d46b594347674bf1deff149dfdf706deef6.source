package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pBundleInstaller
import com.client.xvideos.common.p2p.P2pInboxMerger
import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.p2p.mirrorRoot
import timber.log.Timber
import java.io.File

/** Контракт импорта принятого бандла. */
fun interface BundleImporter {
    suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>)
}

/**
 * Ставит принятый бандл в зеркало inbox, переносит содержимое inbox в main
 * (боевой store) и дёргает refresh. Полуполученный бандл в боевой store не
 * попадает: import зовётся только при «манифест + все файлы».
 *
 * @param storeRootFor корень store по типу (вызывающий подставляет `AppPath.*`).
 * @param refreshFor перечитать список store нужного типа (вызывающий подставляет `saved*.refresh()`).
 * @param inboxRoot staging-папка приёма (`AppPath.p2p_inbox`).
 * @param mainRoot корень `/xvideos` (`AppPath.main`); store-корни лежат внутри него.
 */
class StoreBundleImporter(
    private val storeRootFor: (P2pType) -> File,
    private val refreshFor: (P2pType) -> Unit,
    private val inboxRoot: File,
    private val mainRoot: File,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val storeRoot = storeRootFor(manifest.type)
        val staging = mirrorRoot(inboxRoot, mainRoot, storeRoot)

        // Куда именно легло принятое — вопрос, который без лога выясняется
        // только раскопками в ФС на двух устройствах сразу.
        Timber.i(
            """
            P2P import: тип=${manifest.type}
              mainRoot   = $mainRoot
              storeRoot  = $storeRoot
              inboxRoot  = $inboxRoot
              staging    = $staging
              файлы      = ${manifest.files.map { it.relativePath }}
            """.trimIndent()
        )

        P2pBundleInstaller.install(staging, manifest, receivedFiles)
        P2pInboxMerger.merge(inboxRoot, mainRoot)
        refreshFor(manifest.type)
    }
}
