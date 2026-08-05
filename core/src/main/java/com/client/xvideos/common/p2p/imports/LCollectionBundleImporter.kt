package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pInboxMerger
import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.common.zip.ZipUtils
import java.io.File

/**
 * Приём коллекции L: распаковывает принятый zip в зеркало `inbox/L/Collection`,
 * затем переносит содержимое в боевой store существующим merge (пофайловая
 * перезапись = слияние при совпадении имени коллекции) и дёргает refresh.
 *
 * @param inboxRoot staging-папка приёма (`AppPath.p2p_inbox`).
 * @param mainRoot корень `/xvideos` (`AppPath.main`).
 * @param collectionStoreRoot корень коллекций (`AppPath.l_collection`).
 * @param refresh перечитать список коллекций (`savedL.collection.refreshCollectionList()`).
 */
class LCollectionBundleImporter(
    private val inboxRoot: File,
    private val mainRoot: File,
    private val collectionStoreRoot: File,
    private val refresh: () -> Unit,
) : BundleImporter {

    override suspend fun import(manifest: P2pManifest, receivedFiles: Map<Long, File>) {
        val zip = receivedFiles.values.firstOrNull()
            ?: error("L_COLLECTION bundle has no file")
        val mirror = mirrorRoot(inboxRoot, mainRoot, collectionStoreRoot)
        ZipUtils.unzip(zip, mirror)
        P2pInboxMerger.merge(inboxRoot, mainRoot)
        refresh()
    }
}
