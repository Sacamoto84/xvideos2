package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pInboxMerger
import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.common.zip.ZipUtils
import java.io.File

/**
 * Приём коллекции R: распаковывает принятый zip в зеркало `inbox/R/Collection`,
 * мёржит в боевой store (перезапись при совпадении имени) и дёргает refresh.
 * Зеркало [LCollectionBundleImporter]; отличается корнем store и refresh-колбэком.
 */
class RCollectionBundleImporter(
    private val inboxRoot: File,
    private val mainRoot: File,
    private val collectionStoreRoot: File,
    private val refresh: () -> Unit,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val zip = receivedFiles.values.firstOrNull()
            ?: error("R_COLLECTION bundle has no file")
        val mirror = mirrorRoot(inboxRoot, mainRoot, collectionStoreRoot)
        ZipUtils.unzip(zip, mirror)
        P2pInboxMerger.merge(inboxRoot, mainRoot)
        refresh()
    }
}
