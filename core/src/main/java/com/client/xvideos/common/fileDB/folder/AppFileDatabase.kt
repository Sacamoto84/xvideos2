package com.client.xvideos.common.fileDB.folder

import com.client.xvideos.common.AppPath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFileDatabase @Inject constructor() {
    private val root = AppPath.file_db
    private val volatileCacheMutex = Mutex()
    private var volatileCachesCleared = false

    val cacheUrlStringRam = FileStringCacheTable(FolderTable("$root/cache_url_string_ram"))
    val cacheUrlStringRom = FileStringCacheTable(FolderTable("$root/cache_url_string_rom"))
    val lAlbumPictureCache = FileStringCacheTable(FolderTable("$root/l_album_picture_cache"))
    val lAlbumBundleCache = FileStringCacheTable(FolderTable("$root/l_album_bundle_cache"))
    val rCacheMediaResponse = FileStringCacheTable(FolderTable("$root/r_cache_media_response"))

    val rSearchHistoryExplorerTable = FolderTable("$root/r_search_history_explorer")
    val rSearchHistoryNichesTable = FolderTable("$root/r_search_history_niches")

    suspend fun clearVolatileCachesOnProcessStart() {
        volatileCacheMutex.withLock {
            if (volatileCachesCleared) return
            cacheUrlStringRam.deleteAll()
            volatileCachesCleared = true
        }
    }
}
