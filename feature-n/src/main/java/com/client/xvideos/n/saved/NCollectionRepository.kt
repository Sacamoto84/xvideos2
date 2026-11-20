package com.client.xvideos.n.saved

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.CollectionDB
import com.client.xvideos.common.collectionDB.CollectionName
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.n.model.NPicture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NCollectionRepository @Inject constructor(@ApplicationScope scope: CoroutineScope) {

    private val db by lazy { CollectionDB(AppPath.n_collection, NPicture.serializer()) }
    private val mutex = Mutex()
    private val configs = mutableStateMapOf<String, NCollectionConfig>()

    val collections = mutableStateListOf<CollectionEntity<NPicture>>()

    var currentCollectionName by mutableStateOf<String?>(null)
        private set

    val currentCollectionItems: List<NPicture>
        get() = collections.firstOrNull { it.collection == currentCollectionName }?.items.orEmpty()

    init {
        scope.launch { refresh() }
    }

    suspend fun refresh() = onDisk {
        if (!AppPath.isInitialized) return@onDisk
        val loaded = db.readAllCollections().getOrNull() ?: return@onDisk
        val loadedConfigs = loaded.associate { it.collection to readConfig(it.collection) }
        Snapshot.withMutableSnapshot {
            collections.clear()
            collections.addAll(loaded)
            configs.clear()
            configs.putAll(loadedConfigs)
            if (loaded.none { it.collection == currentCollectionName }) currentCollectionName = null
        }
    }

    suspend fun openCollection(name: String) = onDisk {
        val safeName = CollectionName.normalizeOrNull(name) ?: return@onDisk
        if (collections.none { it.collection == safeName }) reloadCollection(safeName)
        if (collections.any { it.collection == safeName }) currentCollectionName = safeName
    }

    suspend fun closeCollection() = onDisk {
        currentCollectionName = null
    }

    internal suspend fun readPictures(collectionName: String): List<NPicture> = onDisk {
        db.readCollection(collectionName).getOrNull()?.items.orEmpty()
    }

    suspend fun createCollection(name: String): Boolean = mutate {
        val folder = createCollectionFolder(name).getOrElse {
            SnackBar.error(it.message ?: "Ошибка создания коллекции")
            return@mutate false
        }
        reloadCollection(folder.name)
        SnackBar.success("Коллекция ${folder.name} создана")
        true
    }

    suspend fun createCollectionAndAdd(name: String, picture: NPicture): Boolean = mutate {
        val folder = createCollectionFolder(name).getOrElse {
            SnackBar.error(it.message ?: "Ошибка создания коллекции")
            return@mutate false
        }
        val inserted = db.insert(picture.id, folder.name, picture)
        if (inserted.isFailure) folder.delete()
        reloadCollection(folder.name)
        report(inserted, "Добавлено в новую коллекцию", "Ошибка добавления в коллекцию")
    }

    suspend fun deleteCollection(name: String): Boolean = mutate {
        val result = db.deleteCollection(name)
        if (result.getOrDefault(false)) {
            val safeName = requireNotNull(CollectionName.normalizeOrNull(name))
            Snapshot.withMutableSnapshot {
                collections.removeAll { it.collection == safeName }
                configs.remove(safeName)
                if (currentCollectionName == safeName) currentCollectionName = null
            }
        }
        report(result, "Коллекция $name удалена", "Ошибка удаления коллекции")
    }

    suspend fun renameCollection(oldName: String, newName: String): Boolean = mutate {
        val result = db.renameCollection(oldName, newName)
        if (result.getOrDefault(false)) {
            val safeOld = requireNotNull(CollectionName.normalizeOrNull(oldName))
            val safeNew = requireNotNull(CollectionName.normalizeOrNull(newName))
            val renamed = db.readCollection(safeNew).getOrNull()
            val config = readConfig(safeNew)
            Snapshot.withMutableSnapshot {
                collections.removeAll { it.collection == safeOld }
                configs.remove(safeOld)
                if (renamed != null) storeCollection(renamed, config)
                if (currentCollectionName == safeOld) currentCollectionName = renamed?.collection
            }
        }
        report(result, "Коллекция переименована", "Ошибка переименования коллекции")
    }

    suspend fun addToCollection(collectionName: String, picture: NPicture): Boolean = mutate {
        val safeName = CollectionName.normalizeOrNull(collectionName)
        if (safeName == null || !File(AppPath.n_collection, safeName).isDirectory) {
            SnackBar.error("Коллекция больше недоступна")
            return@mutate false
        }
        val result = db.insert(picture.id, safeName, picture)
        if (result.isSuccess) reloadCollection(safeName)
        report(result, "Добавлено в коллекцию", "Ошибка добавления в коллекцию")
    }

    suspend fun removeFromCollection(collectionName: String, pictureId: String): Boolean = mutate {
        val result = db.deleteItem(pictureId, collectionName)
        if (result.getOrDefault(false)) {
            val safeName = requireNotNull(CollectionName.normalizeOrNull(collectionName))
            val config = readConfig(safeName)
            if (config.coverPictureId == pictureId) writeConfig(safeName, config.copy(coverPictureId = null))
            reloadCollection(safeName)
        }
        report(result, "Удалено из коллекции", "Ошибка удаления из коллекции")
    }

    fun getCollectionConfig(collectionName: String): NCollectionConfig =
        CollectionName.normalizeOrNull(collectionName)?.let(configs::get) ?: NCollectionConfig()

    fun getCollectionCoverUrl(collectionName: String, items: List<NPicture>): String? {
        val coverId = getCollectionConfig(collectionName).coverPictureId
        return (items.firstOrNull { it.id == coverId } ?: items.firstOrNull())?.thumbnailUrl
    }

    suspend fun setCollectionCover(collectionName: String, picture: NPicture): Boolean = mutate {
        val safeName = CollectionName.normalizeOrNull(collectionName)
        val items = safeName?.let { db.readCollection(it).getOrNull()?.items }
        if (safeName == null || items?.any { it.id == picture.id } != true) {
            SnackBar.error("Страница больше не входит в коллекцию")
            return@mutate false
        }
        val config = readConfig(safeName).copy(coverPictureId = picture.id)
        val saved = writeConfig(safeName, config)
        if (saved) configs[safeName] = config
        report(Result.success(saved), "Обложка коллекции обновлена", "Ошибка обновления обложки")
    }

    private suspend fun <T> onDisk(action: () -> T): T =
        withContext(Dispatchers.IO) { mutex.withLock { action() } }

    private suspend fun <T> mutate(action: () -> T): T =
        withContext(NonCancellable) { onDisk(action) }

    private fun createCollectionFolder(name: String): Result<File> = runCatching {
        val safeName = requireNotNull(CollectionName.normalizeOrNull(name)) { "Недопустимое имя коллекции" }
        val root = File(AppPath.n_collection)
        check(root.isDirectory || root.mkdirs()) { "Хранилище коллекций недоступно" }
        val folder = File(root, safeName)
        if (!folder.mkdir()) {
            check(!folder.exists()) { "Коллекция «$safeName» уже существует" }
            error("Не удалось создать коллекцию")
        }
        folder
    }

    private fun reloadCollection(name: String) {
        val entry = db.readCollection(name).getOrNull() ?: return
        val config = readConfig(entry.collection)
        Snapshot.withMutableSnapshot { storeCollection(entry, config) }
    }

    private fun storeCollection(entry: CollectionEntity<NPicture>, config: NCollectionConfig) {
        val index = collections.indexOfFirst { it.collection >= entry.collection }
        when {
            index == -1 -> collections.add(entry)
            collections[index].collection == entry.collection -> collections[index] = entry
            else -> collections.add(index, entry)
        }
        configs[entry.collection] = config
    }

    private fun readConfig(safeName: String): NCollectionConfig {
        val file = configFile(safeName)
        if (!file.exists() || file.length() == 0L) return NCollectionConfig()
        return runCatching {
            AppJson.decodeFromString(NCollectionConfig.serializer(), file.readText(Charsets.UTF_8))
        }.getOrElse {
            Timber.w(it, "Не удалось прочитать конфигурацию коллекции $safeName")
            NCollectionConfig()
        }
    }

    private fun writeConfig(safeName: String, config: NCollectionConfig): Boolean = runCatching {
        val file = configFile(safeName)
        check(file.parentFile?.isDirectory == true)
        file.writeTextAtomically(AppJson.encodeToString(NCollectionConfig.serializer(), config))
    }.onFailure {
        Timber.e(it, "Не удалось сохранить конфигурацию коллекции $safeName")
    }.isSuccess

    private fun configFile(safeName: String) =
        File(File(AppPath.n_collection, safeName), "collection_config.json")

    private fun report(result: Result<Boolean>, success: String, error: String): Boolean {
        val succeeded = result.getOrDefault(false)
        if (succeeded) SnackBar.success(success) else SnackBar.error(error)
        return succeeded
    }
}
