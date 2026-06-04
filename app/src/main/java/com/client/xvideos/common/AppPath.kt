package com.client.xvideos.common

import android.content.Context
import android.os.Environment
import java.io.IOException
import java.io.File

private enum class Folder(val value: String) {
    CACHE_DOWNLOAD_RED("Download"),
    RED("R"),
    L("L"),
    X("X")
}

/**
 * Центральное место для путей файлового хранилища приложения.
 *
 * Объект строит структуру каталогов внутри `xvideos` на внешнем хранилище:
 * отдельно для X, RedGifs и L-раздела. Временный L-cache для шаринга хранится
 * во внутреннем cache dir приложения. При первой инициализации создаёт
 * недостающие папки и `.nomedia`, чтобы системная галерея не индексировала
 * служебные медиафайлы приложения.
 */
object AppPath {

    private const val appMain = "xvideos"
    private const val R_NICHES_CACHE_FILE_NAME = "niches.json"

    /**
     * Путь до внешнего хранилища
     */
    val sdcard: String = Environment.getExternalStorageDirectory().toString()

    val main : String = "$sdcard/$appMain"

    val file_db: String = "${main}/DB"

    //--- X ---
    val x_favorites : String = "${main}/${Folder.X.value}/Favorites"
    val x_cache_download : String = "${main}/${Folder.X.value}/Download"

    //--- R ---
    /**
     * Пусть к папке с кешем загруженных файлов для предросмотра
     */
    val r_cache_download : String = "${main}/${Folder.RED.value}/${Folder.CACHE_DOWNLOAD_RED.value}"

    val r_block : String = "${main}/${Folder.RED.value}/Block"

    val r_likes : String = "${main}/${Folder.RED.value}/Likes"
    val r_collection : String = "${main}/${Folder.RED.value}/Collection"
    val r_niches : String = "${main}/${Folder.RED.value}/Niches"
    lateinit var r_nichesCache : String
        private set
    val r_creators : String = "${main}/${Folder.RED.value}/Creators"

    val r_subscriptions: String = "${main}/${Folder.RED.value}/Subscriptions"

    //--- L ---
    val l_likes: String = "${main}/${Folder.L.value}/Likes"
    lateinit var l_cacheDownload: String
        private set
    val l_albums: String = "${main}/${Folder.L.value}/Album"
    val l_collection: String = "${main}/${Folder.L.value}/Collection"

    /**
     * Создаёт базовую структуру директорий при первом обращении к `AppPath`.
     *
     * Kotlin `object` инициализируется лениво, поэтому папки создаются не при
     * запуске процесса, а когда код впервые обращается к одному из путей.
     */
    init {

        println("---AppPath---")
        println("sdcard: $sdcard")

        File(main).mkdirs()
        File(file_db).mkdirs()

        // Создание .nomedia
        val nomedia = File(main, ".nomedia")
        if (!nomedia.exists()) {
            try {
                nomedia.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        File(r_cache_download).mkdirs()

        File(r_block).mkdirs()

        File(r_likes).mkdirs()
        File(r_collection).mkdirs()
        File(r_niches).mkdirs()
        File(r_creators).mkdirs()
        File(r_subscriptions).mkdirs()

        File(l_likes).mkdirs()

        File(l_albums).mkdirs()
        File(l_collection).mkdirs()

        File(x_favorites).mkdirs()
        File(x_cache_download).mkdirs()

    }

    fun initInternalStorage(context: Context) {
        val shareCacheDir = File(context.cacheDir, "L/Share")
        l_cacheDownload = shareCacheDir.absolutePath

        val rNichesCacheDir = File(context.filesDir, "${Folder.RED.value}/NichesCache")
        r_nichesCache = rNichesCacheDir.absolutePath
        rNichesCacheDir.mkdirs()
        migrateLegacyRNichesCache(rNichesCacheDir)

        clearLShareCache()
    }

    private fun migrateLegacyRNichesCache(targetDir: File) {
        val legacyDir = File(main, "${Folder.RED.value}/NichesCache")
        if (!legacyDir.exists()) return

        runCatching {
            val legacyFile = File(legacyDir, R_NICHES_CACHE_FILE_NAME)
            val targetFile = File(targetDir, R_NICHES_CACHE_FILE_NAME)

            if (legacyFile.exists() && !targetFile.exists()) {
                legacyFile.copyTo(targetFile, overwrite = false)
            }

            legacyDir.deleteRecursively()
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun clearLShareCache() {
        if (!::l_cacheDownload.isInitialized) return
        File(l_cacheDownload).deleteRecursively()
        File(l_cacheDownload).mkdirs()
    }

}
