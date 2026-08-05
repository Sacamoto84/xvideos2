package com.client.xvideos.common

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
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
 * Всё дерево лежит во внутренней памяти приложения — `filesDir/store`. Это
 * каталог, недоступный другим приложениям и файловым менеджерам, поэтому пароль
 * на приложении закрывает не только UI, но и сами файлы. Обратная сторона:
 * данные стираются вместе с приложением, и единственный способ их сохранить —
 * ZIP-бэкап (`XlrBackupManager`).
 *
 * Корень — подпапка `store`, а не сам `filesDir`, потому что рядом в `filesDir`
 * лежит `R/NichesCache`. Будь корнем `filesDir`, этот кеш попадал бы внутрь
 * backup-секции `R` и уезжал в каждый архив.
 *
 * Все пути вычисляются от [main], поэтому смена корня переносит всё приложение.
 * Абсолютные пути нигде не сохраняются на диск — файлы адресуются как
 * «корень из AppPath + имя», так что переезд не требует переписывать данные.
 */
object AppPath {

    private const val STORE_DIR = "store"
    private const val R_NICHES_CACHE_FILE_NAME = "niches.json"

    private var root: File? = null

    private val requireRoot: File
        get() = root ?: error(
            "AppPath не инициализирован. AppPath.init(context) должен вызываться " +
                "в начале App.onCreate(), до обращения к путям."
        )

    /** Корень хранилища приложения: `filesDir/store`. */
    val main: String get() = requireRoot.path

    val file_db: String get() = "$main/DB"

    //--- X ---
    val x_favorites: String get() = "$main/${Folder.X.value}/Favorites"
    val x_cache_download: String get() = "$main/${Folder.X.value}/Download"

    //--- R ---
    /**
     * Путь к папке с кешем загруженных файлов для предпросмотра
     */
    val r_cache_download: String get() = "$main/${Folder.RED.value}/${Folder.CACHE_DOWNLOAD_RED.value}"

    val r_block: String get() = "$main/${Folder.RED.value}/Block"

    val r_likes: String get() = "$main/${Folder.RED.value}/Likes"
    val r_collection: String get() = "$main/${Folder.RED.value}/Collection"
    val r_niches: String get() = "$main/${Folder.RED.value}/Niches"

    /** Кеш niches — в `filesDir`, вне [main], чтобы не попадать в бэкапы. */
    lateinit var r_nichesCache: String
        private set

    val r_creators: String get() = "$main/${Folder.RED.value}/Creators"

    val r_subscriptions: String get() = "$main/${Folder.RED.value}/Subscriptions"

    //--- L ---
    val l_likes: String get() = "$main/${Folder.L.value}/Likes"

    /** Временный cache для шаринга — в `cacheDir`, вне [main]. */
    lateinit var l_cacheDownload: String
        private set

    val l_albums: String get() = "$main/${Folder.L.value}/Album"
    val l_collection: String get() = "$main/${Folder.L.value}/Collection"

    //--- P2P staging ---
    /**
     * Временные папки P2P. Содержимое зеркалирует структуру [main]
     * (`inbox/L/Likes/...`), что позволяет переносить принятое в корень
     * одним merge. Очищаются при старте приложения и после успешной передачи.
     */
    val p2p_inbox: String get() = "$main/inbox"
    val p2p_outbox: String get() = "$main/outbox"

    /**
     * Задаёт корень хранилища и создаёт структуру каталогов.
     *
     * Обязан вызываться первым делом в `App.onCreate()`: Hilt-синглтоны
     * (например `AppFileDatabase`) читают пути прямо в конструкторе, а любое
     * обращение до инициализации бросает [IllegalStateException] с пояснением.
     *
     * Делает только то, что обязано быть готово синхронно: назначает пути и
     * создаёт папки. Всё, что может занять заметное время, — в
     * [cleanupTransientDirs].
     */
    fun init(context: Context) {
        root = File(context.filesDir, STORE_DIR)

        File(main).mkdirs()
        File(file_db).mkdirs()

        // `.nomedia` больше не нужен: MediaScanner не ходит во внутреннюю
        // память приложения, индексировать эти файлы некому.

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

        File(p2p_inbox).mkdirs()
        File(p2p_outbox).mkdirs()

        val shareCacheDir = File(context.cacheDir, "${Folder.L.value}/Share")
        l_cacheDownload = shareCacheDir.absolutePath

        val rNichesCacheDir = File(context.filesDir, "${Folder.RED.value}/NichesCache")
        r_nichesCache = rNichesCacheDir.absolutePath
        rNichesCacheDir.mkdirs()

        // Остаётся синхронной намеренно. В обычном случае это один exists() —
        // legacy-каталога нет. Работа появляется только после восстановления
        // старого архива, и она ограничена одним JSON. Зато `SavedRed` в своём
        // init читает этот кеш, а Hilt внедряет его в MainActivity до того, как
        // успел бы отработать любой await, — асинхронная миграция гонялась бы
        // с чтением.
        migrateLegacyRNichesCache(rNichesCacheDir)
    }

    /**
     * Разовая уборка staging-папок при старте процесса.
     *
     * Вынесено из [init], потому что `deleteRecursively` по трём папкам — это
     * обход дерева со всеми принятыми и отданными файлами, а [init] вызывается
     * из `App.onCreate()` на главном потоке. При крупном inbox это заметная
     * задержка перед первым кадром.
     *
     * Вызывающий обязан дождаться завершения, прежде чем что-либо начнёт
     * писать в эти папки: `App.awaitStorageCleanup()`. Иначе приём P2P может
     * стартовать в inbox, который в этот момент удаляется.
     */
    suspend fun cleanupTransientDirs() = withContext(Dispatchers.IO) {
        clearLShareCache()
        clearP2pInbox()
        clearP2pOutbox()
    }

    /**
     * Вытаскивает `niches.json` из старого места внутри [main].
     *
     * Кеш когда-то лежал в `R/NichesCache` рядом с данными. Восстановление
     * старого архива может занести его туда снова, поэтому проверка осталась.
     */
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
            Timber.e(it, "AppPath: не удалось перенести данные из legacy-каталога")
        }
    }

    fun clearLShareCache() {
        if (!::l_cacheDownload.isInitialized) return
        File(l_cacheDownload).deleteRecursively()
        File(l_cacheDownload).mkdirs()
    }

    fun clearP2pInbox() = clearStagingDir(File(p2p_inbox))

    fun clearP2pOutbox() = clearStagingDir(File(p2p_outbox))

    private fun clearStagingDir(dir: File) {
        dir.deleteRecursively()
        dir.mkdirs()
    }

}
