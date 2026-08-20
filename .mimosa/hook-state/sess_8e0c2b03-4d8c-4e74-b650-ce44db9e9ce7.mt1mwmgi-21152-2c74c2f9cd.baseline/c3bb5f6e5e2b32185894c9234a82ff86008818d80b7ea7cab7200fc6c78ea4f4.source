package com.client.xvideos.common.fileDB.folder

import com.client.xvideos.common.AppPath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
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

    /**
     * Ответы лент RedGifs. Единственная таблица со сроком годности.
     *
     * Ключ здесь — полный URL вместе с номером страницы, то есть запись на
     * каждую страницу каждой ленты. Без срока эта папка не переставала расти, а
     * «Топ за неделю» показывал то, что попало в кеш в первый раз, — обновиться
     * ему было неоткуда.
     *
     * Шесть часов: за это время лента заметно меняется, но пролистать её туда и
     * обратно, не выходя из приложения, можно без повторных запросов.
     */
    val rCacheMediaResponse = FileStringCacheTable(
        table = FolderTable("$root/r_cache_media_response"),
        ttlMs = TimeUnit.HOURS.toMillis(6)
    )

    val rSearchHistoryExplorerTable = FolderTable("$root/r_search_history_explorer")
    val rSearchHistoryNichesTable = FolderTable("$root/r_search_history_niches")

    suspend fun clearVolatileCachesOnProcessStart() {
        volatileCacheMutex.withLock {
            if (volatileCachesCleared) return
            cacheUrlStringRam.deleteAll()
            volatileCachesCleared = true
        }
    }

    /**
     * Выметает просроченные записи из таблиц со сроком годности.
     *
     * Отдельно от [clearVolatileCachesOnProcessStart]: ту ждут перед открытием
     * главного экрана, а здесь обход каталога, размер которого заранее не
     * известен. Вызывать в фоне.
     */
    suspend fun deleteExpiredCaches() {
        rCacheMediaResponse.deleteExpired()
    }
}
