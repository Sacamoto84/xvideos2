package com.client.xvideos.r.common.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
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

    var collectionItemGifInfo by mutableStateOf<GifsInfo?>(null)

    val selectedCollection = MutableStateFlow<String?>(null)
    //-------------------------------------------

    private var refreshJob: Job? = null

    override fun addCollection(item: GifsInfo, collectionName: String) {
        val safeItem = item.sanitizeOrNull() ?: run {
            SnackBar.error("Collection add error: empty id")
            return
        }
        Timber.i("R_Saved_Collection addCollection() item:${safeItem.id} collectionName:$collectionName")
        scope.launch(Dispatchers.IO) {
            collectionDb.insert(safeItem.id, collectionName, safeItem)
                .onSuccess { refreshCollectionList() }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления GIF в коллекцию $collectionName ${e.message}")
                }
        }
    }

    override fun deleteItemFromCollection(itemId: String, collectionName: String) {
        if (itemId.isBlank() || collectionName.isBlank()) return
        Timber.i("R_Saved_Collection deleteItemFromCollection() item:${itemId} collectionName:$collectionName")
        scope.launch(Dispatchers.IO) {
            collectionDb.deleteItem(itemId, collectionName)
                .onSuccess {
                    SnackBar.success("GIF удален из коллекции $collectionName")
                    refreshCollectionList()
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления GIF из коллекции $collectionName ${e.message}") }
        }
    }

    override fun deleteCollection(collectionName: String) {
        if (collectionName.isBlank()) return
        scope.launch(Dispatchers.IO) {
            collectionDb.deleteCollection(collectionName)
                .onSuccess {
                    SnackBar.success("Коллекция $collectionName удалена")
                    refreshCollectionList()
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления коллекции $collectionName ${e.message}") }
        }
    }

    override fun createCollection(collectionName: String) {
        if (collectionName.isBlank()) return
        Timber.i("R_Saved_Collection createCollection() collectionName:$collectionName")
        scope.launch(Dispatchers.IO) {
            collectionDb.create(collectionName)
                .onSuccess {
                    SnackBar.success("Коллекция $collectionName создана")
                    refreshCollectionList()
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка создания коллекции $collectionName ${e.message}")
                }
        }
    }

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
}
