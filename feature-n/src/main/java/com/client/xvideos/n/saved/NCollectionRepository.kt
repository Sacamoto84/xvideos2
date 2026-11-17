package com.client.xvideos.n.saved

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.CollectionDB
import com.client.xvideos.common.collectionDB.CollectionName
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.io.writeTextAtomically
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.n.model.NPicture
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий пользовательских коллекций страниц/картинок nhentai на базе CollectionDB.
 */
@Singleton
class NCollectionRepository @Inject constructor() {

    private val dbPath: String by lazy {
        File(AppPath.n_collection).apply { mkdirs() }.absolutePath
    }

    private val db: CollectionDB<NPicture> by lazy {
        CollectionDB(dbPath, NPicture.serializer())
    }

    /** Список всех существующих коллекций с элементами. */
    val collections = mutableStateListOf<CollectionEntity<NPicture>>()

    /** Имя открытой в данный момент коллекции или null, если открыт список коллекций. */
    var currentCollectionName by mutableStateOf<String?>(null)
        private set

    /** Элементы текущей открытой коллекции. */
    val currentCollectionItems = mutableStateListOf<NPicture>()

    init {
        refresh()
    }

    fun refresh() {
        if (!AppPath.isInitialized) return
        val list = db.readAllCollections().getOrDefault(emptyList())
        collections.clear()
        collections.addAll(list)

        val current = currentCollectionName
        if (current != null) {
            val entity = list.firstOrNull { it.collection.equals(current, ignoreCase = true) }
            currentCollectionItems.clear()
            if (entity != null) {
                currentCollectionItems.addAll(entity.items)
            } else {
                currentCollectionName = null
            }
        }
    }

    fun openCollection(name: String) {
        currentCollectionName = name
        val entity = db.readCollection(name).getOrNull()
        currentCollectionItems.clear()
        if (entity != null) {
            currentCollectionItems.addAll(entity.items)
        }
    }

    fun closeCollection() {
        currentCollectionName = null
        currentCollectionItems.clear()
    }

    fun createCollection(name: String): Boolean {
        val result = db.create(name)
        if (result.isSuccess) {
            refresh()
            SnackBar.success("Коллекция $name создана")
            return true
        }
        SnackBar.error("Ошибка создания коллекции")
        return false
    }

    fun deleteCollection(name: String): Boolean {
        val result = db.deleteCollection(name)
        if (result.isSuccess) {
            if (currentCollectionName.equals(name, ignoreCase = true)) {
                closeCollection()
            }
            refresh()
            SnackBar.success("Коллекция $name удалена")
            return true
        }
        SnackBar.error("Ошибка удаления коллекции")
        return false
    }

    fun renameCollection(oldName: String, newName: String): Boolean {
        val result = db.renameCollection(oldName, newName)
        if (result.isSuccess) {
            if (currentCollectionName.equals(oldName, ignoreCase = true)) {
                currentCollectionName = newName
            }
            refresh()
            SnackBar.success("Коллекция переименована")
            return true
        }
        SnackBar.error("Ошибка переименования коллекции")
        return false
    }

    fun addToCollection(collectionName: String, picture: NPicture): Boolean {
        val result = db.insert(picture.id, collectionName, picture)
        if (result.isSuccess) {
            refresh()
            SnackBar.success("Добавлено в коллекцию")
            return true
        }
        SnackBar.error("Ошибка добавления в коллекцию")
        return false
    }

    fun removeFromCollection(collectionName: String, pictureId: String): Boolean {
        val result = db.deleteItem(pictureId, collectionName)
        if (result.isSuccess) {
            val config = getCollectionConfig(collectionName)
            if (config.coverPictureId == pictureId) {
                saveCollectionConfig(collectionName, config.copy(coverPictureId = null))
            }
            refresh()
            SnackBar.success("Удалено из коллекции")
            return true
        }
        SnackBar.error("Ошибка удаления из коллекции")
        return false
    }

    /**
     * Читает конфигурацию коллекции [collectionName] из директории коллекции.
     */
    fun getCollectionConfig(collectionName: String): NCollectionConfig {
        val safeName = CollectionName.normalizeOrNull(collectionName) ?: return NCollectionConfig()
        val file = File(File(AppPath.n_collection, safeName), CONFIG_FILE_NAME)
        if (!file.exists() || file.length() == 0L) return NCollectionConfig()
        return try {
            val text = file.readText(Charsets.UTF_8)
            AppJson.decodeFromString(NCollectionConfig.serializer(), text)
        } catch (e: Exception) {
            Timber.w(e, "Не удалось прочитать конфигурацию коллекции $safeName")
            NCollectionConfig()
        }
    }

    /**
     * Сохраняет конфигурацию коллекции [collectionName] в её директории.
     */
    private fun saveCollectionConfig(collectionName: String, config: NCollectionConfig): Boolean {
        val safeName = CollectionName.normalizeOrNull(collectionName) ?: return false
        val dir = File(AppPath.n_collection, safeName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, CONFIG_FILE_NAME)
        return try {
            val jsonText = AppJson.encodeToString(NCollectionConfig.serializer(), config)
            file.writeTextAtomically(jsonText)
            true
        } catch (e: Exception) {
            Timber.e(e, "Не удалось сохранить конфигурацию коллекции $safeName")
            false
        }
    }

    /**
     * Возвращает URL обложки коллекции: вручную назначенную обложку или первый доступный элемент.
     */
    fun getCollectionCoverUrl(collectionName: String, items: List<NPicture>): String? {
        val config = getCollectionConfig(collectionName)
        if (!config.coverPictureId.isNullOrEmpty()) {
            val found = items.firstOrNull { it.id == config.coverPictureId }
            if (found != null) {
                return found.thumbnailUrl
            }
        }
        return items.firstOrNull()?.thumbnailUrl
    }

    /**
     * Назначает выбранную картинку [picture] обложкой коллекции [collectionName],
     * сохраняя её ID в файле конфигурации коллекции без изменения порядка элементов.
     */
    fun setCollectionCover(collectionName: String, picture: NPicture): Boolean {
        val currentConfig = getCollectionConfig(collectionName)
        val newConfig = currentConfig.copy(coverPictureId = picture.id)
        val saved = saveCollectionConfig(collectionName, newConfig)
        if (saved) {
            refresh()
            SnackBar.success("Обложка коллекции обновлена")
            return true
        }
        SnackBar.error("Ошибка обновления обложки")
        return false
    }

    companion object {
        private const val CONFIG_FILE_NAME = "collection_config.json"
    }
}
