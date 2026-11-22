package com.client.xvideos.common.fileDB.folder

import com.client.xvideos.common.AppPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFileDatabase @Inject constructor() {
    private val root = AppPath.file_db
    val cacheUrlStringRom = FileStringCacheTable(FolderTable("$root/cache_url_string_rom"))
    val lAlbumPictureCache = FileStringCacheTable(FolderTable("$root/l_album_picture_cache"))
    val lAlbumBundleCache = FileStringCacheTable(FolderTable("$root/l_album_bundle_cache"))

    /**
     * Ответы лент R. Единственная таблица со сроком годности.
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
    val xSearchHistoryTable = FolderTable("$root/x_search_history")

    /** Удаляет старый дисковый кэш HTML X и просроченные записи R в фоне. */
    suspend fun cleanupObsoleteCaches() {
        withContext(Dispatchers.IO) { File(root, "cache_url_string_ram").deleteRecursively() }
        rCacheMediaResponse.deleteExpired()
    }
}
