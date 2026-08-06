package com.client.xvideos.p2p

import android.content.Context
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.p2p.imports.BundleImporter
import com.client.xvideos.common.p2p.imports.LCollectionBundleImporter
import com.client.xvideos.common.p2p.imports.RCollectionBundleImporter
import com.client.xvideos.common.p2p.imports.StoreBundleImporter
import com.client.xvideos.r.common.p2p.RLikesBundleImporter
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber
import java.io.File

/**
 * Импортёр принятых бандлов: раскладывает содержимое по хранилищам разделов и
 * обновляет их списки.
 *
 * Живёт в точке сборки, а не в базовом слое: единственное место, которому нужны
 * сразу и `SavedL`, и `SavedRed`. Ставится в `P2pReceiveManager.importerFactory`
 * при старте процесса.
 */
fun sectionBundleImporter(context: Context): BundleImporter {
    val entryPoint = EntryPointAccessors
        .fromApplication(context.applicationContext, P2pRefreshEntryPoint::class.java)

    Timber.i("P2P приём: корень хранилища ${AppPath.main}")

    val storeImporter = StoreBundleImporter(
        storeRootFor = { type ->
            when (type) {
                P2pType.X -> File(AppPath.x_cache_download)
                // R сюда не попадает — идёт через RLikesBundleImporter.
                P2pType.R -> File(AppPath.r_cache_download)
                P2pType.L -> File(AppPath.l_likes)
                P2pType.L_ALBUM -> File(AppPath.l_albums)
                P2pType.L_COLLECTION -> File(AppPath.l_collection)
                P2pType.R_COLLECTION -> File(AppPath.r_collection)
            }
        },
        refreshFor = { type ->
            // X: экран Saved перечитывает список при открытии.
            when (type) {
                P2pType.L -> entryPoint.savedL().likes.refresh()
                P2pType.L_ALBUM -> entryPoint.savedL().albums.refresh()
                else -> Unit
            }
        },
        inboxRoot = File(AppPath.p2p_inbox),
        mainRoot = File(AppPath.main),
    )

    // R: «лайк» — запись метаданных в FileDB, файлы не раскладываем
    // (LikesTab рендерит по URL); list.add сам обновляет Compose-state.
    val rLikesImporter = RLikesBundleImporter(addLike = { entryPoint.savedRed().likes.add(it) })

    // L_COLLECTION: принятый zip распаковывается в зеркало inbox и мёржится в store.
    val lCollectionImporter = LCollectionBundleImporter(
        inboxRoot = File(AppPath.p2p_inbox),
        mainRoot = File(AppPath.main),
        collectionStoreRoot = File(AppPath.l_collection),
        refresh = { entryPoint.savedL().collection.refreshCollectionList() },
    )

    // R_COLLECTION: принятый zip распаковывается в зеркало inbox и мёржится в R-store.
    val rCollectionImporter = RCollectionBundleImporter(
        inboxRoot = File(AppPath.p2p_inbox),
        mainRoot = File(AppPath.main),
        collectionStoreRoot = File(AppPath.r_collection),
        refresh = { entryPoint.savedRed().collections.refreshCollectionList() },
    )

    return BundleImporter { manifest, files ->
        val branch = when (manifest.type) {
            P2pType.R -> "RLikesBundleImporter (файлы не раскладываются)"
            P2pType.L_COLLECTION -> "LCollectionBundleImporter"
            P2pType.R_COLLECTION -> "RCollectionBundleImporter"
            else -> "StoreBundleImporter"
        }
        Timber.i(
            "P2P приём: тип=${manifest.type} -> $branch; " +
                "файлы манифеста=${manifest.files.map { it.relativePath }}"
        )

        when (manifest.type) {
            P2pType.R -> rLikesImporter.import(manifest, files)
            P2pType.L_COLLECTION -> lCollectionImporter.import(manifest, files)
            P2pType.R_COLLECTION -> rCollectionImporter.import(manifest, files)
            else -> storeImporter.import(manifest, files)
        }
    }
}
