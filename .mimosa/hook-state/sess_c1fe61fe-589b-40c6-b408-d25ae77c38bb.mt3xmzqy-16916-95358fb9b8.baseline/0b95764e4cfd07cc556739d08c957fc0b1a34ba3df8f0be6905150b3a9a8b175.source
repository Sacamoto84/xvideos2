package com.client.xvideos.r.common.saved

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.model.LinkCollectionStore
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.model.sanitizeOrNull
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber

class R_Saved_Collection : LinkCollectionStore<GifsInfo>(
    AppPath.r_collection,
    GifsInfo::class.java
)
{

    override fun addCollection(item: GifsInfo, collectionName: String) {
        val safeItem = item.sanitizeOrNull() ?: run {
            SnackBar.error("Collection add error: empty id")
            return
        }
        Timber.i("R_Saved_Collection addCollection() item:${safeItem.id} collectionName:$collectionName")
        collectionDb.insert(safeItem.id, collectionName, safeItem)
        refreshCollectionList()
    }

    override fun deleteItemFromCollection(itemId: String, collectionName: String) {
        Timber.i("R_Saved_Collection deleteItemFromCollection() item:${itemId} collectionName:$collectionName")
        collectionDb.deleteItem(itemId, collectionName)
            .onSuccess {
                SnackBar.success("GIF удален из коллекции $collectionName")
                refreshCollectionList()
            }
            .onFailure { e -> SnackBar.error("Ошибка удаления GIF из коллекции $collectionName ${e.message}") }
    }

    override fun deleteCollection(collectionName: String) {
            collectionDb.deleteCollection(collectionName)
            .onSuccess {
                SnackBar.success("Коллекция $collectionName удалена")
                refreshCollectionList()
            }
            .onFailure { e -> SnackBar.error("Ошибка удаления коллекции $collectionName ${e.message}") }
    }

    override fun createCollection(collectionName: String) {
        Timber.i("R_Saved_Collection createCollection() collectionName:$collectionName")
            collectionDb.create(collectionName)
            .onSuccess {
                SnackBar.success("Коллекция $collectionName создана")
                refreshCollectionList()
            }
            .onFailure { e ->
                SnackBar.error("Ошибка создания коллекции $collectionName ${e.message}")
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun refreshCollectionList() {
        // Номер берётся до чтения диска: два параллельных refresh иначе
        // разложатся в порядке завершения, а не запуска, и устаревший список
        // ляжет поверх свежего.
        val seq = nextLoadSeq()
        val a = collectionDb.readAllCollections()
        if (a.isSuccess) {
            publish(
                seq,
                a.getOrThrow().map { collection ->
                    collection.copy(items = collection.items.sanitizeGifsInfoList())
                }
            )
        } else {
            SnackBar.error("Ошибка чтения коллекций ${a.exceptionOrNull()?.message}")
        }
    }

}
