package com.client.xvideos.common.collectionDB.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.collectionDB.CollectionDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicLong

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

    /**
     * Номер загрузки и последний опубликованный номер — тот же приём, что в
     * [com.client.xvideos.common.fileDB.FileDB]. Наследник обязан брать номер
     * [nextLoadSeq] до чтения диска и публиковать через [publish]: иначе два
     * параллельных refresh разложатся в порядке завершения, а не запуска.
     *
     * Публикация стоит вне лока `CollectionDB` намеренно: `replaceWith` берёт
     * снапшот-лок Compose, и захват `lock -> snapshotLock` встретился бы с
     * обратным порядком у кода, который зовёт хранилище из-под снапшота.
     */
    private val loadSeq = AtomicLong(0)
    private val publishLock = Any()
    private var publishedSeq = 0L

    /** Номер очередной загрузки. Берётся до чтения диска. */
    protected fun nextLoadSeq(): Long = loadSeq.incrementAndGet()

    /** Публикует результат загрузки [seq], если он не устарел. */
    protected fun publish(seq: Long, items: List<CollectionEntity<T>>) {
        synchronized(publishLock) {
            if (seq > publishedSeq) {
                publishedSeq = seq
                collectionList.replaceWith(items)
            }
        }
    }

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
