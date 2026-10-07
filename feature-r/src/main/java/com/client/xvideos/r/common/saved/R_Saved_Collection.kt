package com.client.xvideos.r.common.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.CollectionName
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.collectionDB.model.LinkCollectionStore
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.model.sanitizeOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Хранилище пользовательских именованных коллекций роликов R.
 *
 * Наследует [LinkCollectionStore] и организует хранение папок коллекций в `AppPath.r_collection`.
 * Управляет состоянием UI-диалогов создания и добавления элементов в коллекции.
 *
 * @param scope Корутин-скоп для асинхронных операций с диском.
 */
@Stable
class R_Saved_Collection(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : LinkCollectionStore<GifsInfo>(
    AppPath.r_collection,
    GifsInfo.serializer()
)
{
    //----- Диалоги (UI-состояние R-раздела) -----
    /** Отобразить диалог коллекции (выбор куда положить элемент) */
    var visibleDialog by mutableStateOf(false)

    /** Отобразить диалог создания новой коллекции */
    var visibleDialogCreateNew by mutableStateOf(false)

    /** Медиаэлемент, над которым сейчас открыт диалог добавления в коллекцию. */
    var collectionItemGifInfo by mutableStateOf<GifsInfo?>(null)

    /** Название текущей выбранной пользователем коллекции. */
    val selectedCollection = MutableStateFlow<String?>(null)
    //-------------------------------------------

    private var refreshJob: Job? = null

    /**
     * Добавляет медиаэлемент [item] в именованную коллекцию [collectionName].
     */
    override fun addCollection(item: GifsInfo, collectionName: String) {
        val safeItem = item.sanitizeOrNull() ?: run {
            SnackBar.error("Collection add error: empty id")
            return
        }
        Timber.i("R_Saved_Collection addCollection() item:${safeItem.id} collectionName:$collectionName")
        scope.launch(Dispatchers.IO) {
            collectionDb.insert(safeItem.id, collectionName, safeItem)
                .onSuccess { refreshCollection(collectionName) }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления GIF в коллекцию $collectionName ${e.message}")
                }
        }
    }

    /**
     * Удаляет медиаэлемент с [itemId] из коллекции [collectionName].
     */
    override fun deleteItemFromCollection(itemId: String, collectionName: String) {
        if (itemId.isBlank() || collectionName.isBlank()) return
        Timber.i("R_Saved_Collection deleteItemFromCollection() item:${itemId} collectionName:$collectionName")
        scope.launch(Dispatchers.IO) {
            collectionDb.deleteItem(itemId, collectionName)
                .onSuccess {
                    SnackBar.success("GIF удален из коллекции $collectionName")
                    refreshCollection(collectionName)
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления GIF из коллекции $collectionName ${e.message}") }
        }
    }

    /**
     * Полностью удаляет коллекцию [collectionName] и все её элементы.
     */
    override fun deleteCollection(collectionName: String) {
        if (collectionName.isBlank()) return
        scope.launch(Dispatchers.IO) {
            collectionDb.deleteCollection(collectionName)
                .onSuccess {
                    SnackBar.success("Коллекция $collectionName удалена")
                    refreshCollection(collectionName)
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления коллекции $collectionName ${e.message}") }
        }
    }

    /**
     * Создает новую пустую именованную коллекцию [collectionName].
     */
    override fun createCollection(collectionName: String) {
        if (collectionName.isBlank()) return
        Timber.i("R_Saved_Collection createCollection() collectionName:$collectionName")
        scope.launch(Dispatchers.IO) {
            collectionDb.create(collectionName)
                .onSuccess {
                    SnackBar.success("Коллекция $collectionName создана")
                    refreshCollection(collectionName)
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка создания коллекции $collectionName ${e.message}")
                }
        }
    }

    /**
     * Перечитывает с диска одну коллекцию [collectionName] и ставит её на место
     * в списке; коллекция, которой на диске больше нет, из списка уходит.
     *
     * Раньше любая правка — элемент добавлен или удалён, коллекция создана или
     * удалена — заканчивалась чтением всех коллекций со всеми элементами.
     */
    private suspend fun refreshCollection(collectionName: String) {
        val name = CollectionName.normalizeOrNull(collectionName)
        val read = collectionDb.readCollection(collectionName).getOrNull()
        if (name == null) {
            refreshCollectionList()
            return
        }
        val entity = read?.let { it.copy(items = it.items.sanitizeGifsInfoList()) }
        withContext(Dispatchers.Main) {
            // Полное чтение ещё идёт: оно началось до этой правки, а список в
            // памяти до его публикации неполон. Пусть перечитает всё заново.
            if (refreshJob?.isActive == true) {
                refreshCollectionList()
                return@withContext
            }
            val others = collectionList.filter { it.collection != name }
            val merged = if (entity == null) others else (others + entity).sortedBy { it.collection }
            publish(nextLoadSeq(), merged)
        }
    }

    /**
     * Асинхронно перечитывает с диска все коллекции и их элементы,
     * выполняет санитацию списков и публикует результат на главный поток.
     */
    override fun refreshCollectionList() {
        val seq = nextLoadSeq()
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            val collectionsResult = collectionDb.readAllCollections()
            if (collectionsResult.isSuccess) {
                val collections = collectionsResult.getOrThrow()
                val items = if (collections.isEmpty()) {
                    emptyList()
                } else {
                    collections.map { collection ->
                        if (collection.items.isEmpty()) collection
                        else collection.copy(items = collection.items.sanitizeGifsInfoList())
                    }
                }
                withContext(Dispatchers.Main) {
                    publish(seq, items)
                }
            } else {
                SnackBar.error("Ошибка чтения коллекций ${collectionsResult.exceptionOrNull()?.message}")
            }
        }
    }

    /** Проверяет, пуст ли список коллекций. */
    val isEmpty: Boolean get() = collectionList.isEmpty()

    /** Проверяет наличие хотя бы одной коллекции. */
    val isNotEmpty: Boolean get() = collectionList.isNotEmpty()

    /** Количество созданных коллекций. */
    val collectionsCount: Int get() = collectionList.size

    /** Проверяет наличие коллекции с именем [name]. */
    fun containsCollection(name: String): Boolean =
        name.isNotBlank() && collectionList.any { it.collection == name }

    /** Поиск коллекции по ее имени [name]. */
    fun findCollectionByNameOrNull(name: String?): CollectionEntity<GifsInfo>? =
        if (name.isNullOrBlank()) null else collectionList.firstOrNull { it.collection == name }

    /** Проверяет, содержится ли медиаэлемент с [itemId] хотя бы в одной коллекции. */
    fun containsItem(itemId: String): Boolean =
        itemId.isNotBlank() && collectionList.any { c -> c.items.any { it.id == itemId } }
}
