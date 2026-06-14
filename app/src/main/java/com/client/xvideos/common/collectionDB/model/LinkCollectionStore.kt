package com.client.xvideos.common.collectionDB.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.collectionDB.CollectionDB
import com.client.xvideos.common.snackbar.SnackBar
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Базовое хранилище коллекций «по ссылке».
 *
 * Каждый элемент сериализуется как JSON-файл `<id>.collection` внутри папки
 * `<path>/<collectionName>/`. В отличие от L-стороны, медиа-файлы здесь не
 * скачиваются — хранятся только ссылки на удалённый ресурс.
 *
 * Этот контракт сейчас используется только в R-разделе (для GifsInfo);
 * раньше класс назывался `ISavedLCollection`, что вводило в заблуждение.
 */
abstract class LinkCollectionStore<T>(
    path: String,
    clazz: Class<T>
) {

    val collectionDb = CollectionDB<T>(path, clazz)

    var collectionList = mutableStateListOf<CollectionEntity<T>>()

    //----- Диалоги -----
    /** Отобразить диалог коллекции (выбор куда положить элемент) */
    var visibleDialog by mutableStateOf(false)

    /** Отобразить диалог создания новой коллекции */
    var visibleDialogCreateNew by mutableStateOf(false)
    //-------------------

    var collectionItemGifInfo by mutableStateOf<T?>(null)

    var selectedCollection = MutableStateFlow<String?>(null)

    abstract fun addCollection(item: T, collectionName: String)

    abstract fun deleteItemFromCollection(itemId: String, collectionName: String)

    abstract fun deleteCollection(collectionName: String)

    abstract fun createCollection(collectionName: String)

    abstract fun refreshCollectionList()

    open fun renameCollection(oldName: String, newName: String): Boolean =
        collectionDb.renameCollection(oldName, newName).fold(
            onSuccess = { ok ->
                if (ok) {
                    SnackBar.success("Коллекция переименована")
                    refreshCollectionList()
                } else {
                    SnackBar.error("Коллекция не найдена")
                }
                ok
            },
            onFailure = { e ->
                SnackBar.error("Ошибка переименования: ${e.message}")
                false
            }
        )
}
