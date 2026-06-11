package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pBundleInstaller
import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pType
import java.io.File

/** Контракт импорта принятого бандла. */
fun interface BundleImporter {
    suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>)
}

/**
 * Кладёт принятый бандл в store нужного типа и дёргает refresh.
 *
 * @param storeRootFor корень store по типу (вызывающий подставляет `AppPath.*`).
 * @param refreshFor перечитать список store нужного типа (вызывающий подставляет `saved*.refresh()`).
 */
class StoreBundleImporter(
    private val storeRootFor: (P2pType) -> File,
    private val refreshFor: (P2pType) -> Unit,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val root = storeRootFor(manifest.type)
        P2pBundleInstaller.install(root, manifest, receivedFiles)
        refreshFor(manifest.type)
    }
}
