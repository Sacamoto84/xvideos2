package com.client.xvideos.l.featured.saved

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.net.Luscious
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Тонкий holder состояния для раздела «Collection» в L.
 *
 * Файловая система выведена в [LCollectionFs.lReadCollections] / [lReadCollectionItems] /
 * [lFindCollectionItemFolder], сетевая часть — в [lPersistPicsDetailsToFolder].
 * Этот класс держит только public API + Compose state и оркестрирует вызовы.
 */
class SavedL_Collection(
    private val scope: CoroutineScope,
    private val luscious: Luscious
) {

    val listUrl = mutableStateListOf<PicsDetails>()
    val collectionList = mutableStateListOf<LCollectionEntity>()
    val duplicateGroups = mutableStateListOf<LCollectionDuplicateGroup>()
    val smartCollectionCandidates = mutableStateListOf<LSmartCollectionCandidate>()

    private val progress = LDownloadProgress(scope)
    val percentDownload: StateFlow<Float> = progress.percentDownload

    var currentCollectionName by mutableStateOf<String?>(null)
    var sortOrder by mutableStateOf(LCollectionSortOrder.RECENT)

    //----- Dialogs -----
    var visibleDialog by mutableStateOf(false)
    var visibleDialogCreateNew by mutableStateOf(false)
    var collectionItemGifInfo by mutableStateOf<PicsDetails?>(null)
    val collectionItemsPendingAdd = mutableStateListOf<PicsDetails>()
    //-------------------

    init {
        refreshCollectionList()
    }

    /* ---------- Список коллекций ---------- */

    fun refreshCollectionList() {
        // Обход каталога коллекций (с подсчётом элементов/дублей и чтением
        // metadata.json) — на IO; обновление Compose-state — на Main, иначе ANR.
        val order = sortOrder
        scope.launch(Dispatchers.IO) {
            Timber.i("SavedL_Collection refreshCollectionList()")
            val items = try {
                lReadCollections(File(AppPath.l_collection), order)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SavedL_Collection refreshCollectionList() Ошибка получения списка коллекций")
                SnackBar.error("Ошибка получения списка коллекций")
                return@launch
            }
            withContext(Dispatchers.Main) {
                collectionList.clear()
                collectionList.addAll(items)
                Timber.i("SavedL_Collection refreshCollectionList() collections:${items.size}")
            }
        }
    }

    fun applySortOrder(order: LCollectionSortOrder) {
        sortOrder = order
        refreshCollectionList()
    }

    fun createCollection(collectionName: String) {
        Timber.i("SavedL_Collection createCollection() collectionName:$collectionName")
        val collectionRoot = File(AppPath.l_collection, collectionName)
        if (collectionRoot.exists()) {
            SnackBar.error("Коллекция уже существует")
            return
        }
        collectionRoot.mkdirs()
        SnackBar.success("Коллекция $collectionName создана")
        refreshCollectionList()
    }

    fun deleteCollection(collectionName: String) {
        Timber.i("SavedL_Collection deleteCollection() collectionName:$collectionName")
        val collectionRoot = File(AppPath.l_collection, collectionName)
        if (collectionRoot.deleteRecursively()) {
            if (currentCollectionName == collectionName) {
                currentCollectionName = null
                listUrl.clear()
            }
            SnackBar.success("Коллекция $collectionName удалена")
            refreshCollectionList()
        } else {
            SnackBar.error("Ошибка удаления коллекции $collectionName")
        }
    }

    fun renameCollection(oldName: String, newName: String): Boolean {
        Timber.i("SavedL_Collection renameCollection() oldName:$oldName newName:$newName")
        val trimmedNewName = newName.trim()
        if (trimmedNewName.isBlank()) {
            SnackBar.error("Название коллекции не может быть пустым")
            return false
        }
        if (oldName == trimmedNewName) {
            return true
        }

        val oldRoot = File(AppPath.l_collection, oldName)
        val newRoot = File(AppPath.l_collection, trimmedNewName)
        if (!oldRoot.exists()) {
            SnackBar.error("Коллекция не найдена")
            return false
        }
        if (newRoot.exists()) {
            SnackBar.error("Коллекция уже существует")
            return false
        }

        val renamed = oldRoot.renameTo(newRoot)
        if (renamed) {
            if (currentCollectionName == oldName) {
                currentCollectionName = trimmedNewName
                refresh()
            }
            SnackBar.success("Коллекция переименована")
            refreshCollectionList()
        } else {
            SnackBar.error("Ошибка переименования коллекции")
        }
        return renamed
    }

    /* ---------- Текущая коллекция ---------- */

    fun setCollection(collectionName: String) {
        currentCollectionName = collectionName
        refresh()
    }

    fun refresh() {
        val collectionName = currentCollectionName ?: return
        scope.launch(Dispatchers.IO) {
            Timber.i("SavedL_Collection refresh() collection:$collectionName")
            val items = try {
                lReadCollectionItems(File(AppPath.l_collection, collectionName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SavedL_Collection refresh() Ошибка получения списка коллекции")
                SnackBar.error("Ошибка получения списка коллекции")
                return@launch
            }
            withContext(Dispatchers.Main) {
                listUrl.clear()
                listUrl.addAll(items)
                Timber.i("SavedL_Collection refresh() files:${items.size}")
            }
        }
        refreshDuplicates(collectionName)
    }

    fun refreshDuplicates(collectionName: String? = currentCollectionName) {
        val name = collectionName ?: return
        scope.launch(Dispatchers.IO) {
            val groups = lReadCollectionDuplicateGroups(File(AppPath.l_collection, name))
            withContext(Dispatchers.Main) {
                duplicateGroups.clear()
                duplicateGroups.addAll(groups)
            }
        }
    }

    /* ---------- Элементы ---------- */

    fun beginAddToCollection(item: PicsDetails) {
        collectionItemsPendingAdd.clear()
        collectionItemsPendingAdd.add(item)
        collectionItemGifInfo = item
        visibleDialog = true
    }

    fun beginAddManyToCollection(items: List<PicsDetails>) {
        collectionItemsPendingAdd.clear()
        collectionItemsPendingAdd.addAll(items.distinctBy { lPicsDetailsIdentityKey(it) })
        collectionItemGifInfo = collectionItemsPendingAdd.firstOrNull()
        visibleDialog = collectionItemsPendingAdd.isNotEmpty()
    }

    fun addPendingToCollection(collectionName: String) {
        val items = collectionItemsPendingAdd.toList()
            .ifEmpty { listOfNotNull(collectionItemGifInfo) }
        addAll(items, collectionName)
        visibleDialog = false
        collectionItemsPendingAdd.clear()
        collectionItemGifInfo = null
    }

    fun add(item: PicsDetails, collectionName: String) {
        addAll(listOf(item), collectionName)
    }

    fun addAll(items: List<PicsDetails>, collectionName: String) {
        val uniqueItems = items.distinctBy { lPicsDetailsIdentityKey(it) }
        if (uniqueItems.isEmpty()) return

        Timber.i("SavedL_Collection addAll() count:${uniqueItems.size} collection:$collectionName")

        scope.launch(Dispatchers.IO) {
            var successCount = 0
            var errorCount = 0

            uniqueItems.forEach { item ->
                lPersistPicsDetailsToFolder(
                    item = item,
                    root = File(AppPath.l_collection, collectionName),
                    luscious = luscious,
                    progress = progress
                ).onSuccess {
                    successCount++
                }.onFailure {
                    errorCount++
                    Timber.e(it, "SavedL_Collection addAll() error")
                }
            }

            withContext(Dispatchers.Main) {
                when {
                    successCount > 0 && errorCount == 0 ->
                        SnackBar.success("Добавлено в коллекцию: $successCount")
                    successCount > 0 ->
                        SnackBar.info("Добавлено: $successCount, ошибок: $errorCount")
                    else ->
                        SnackBar.error("Ошибка добавления в коллекцию")
                }

                refreshCollectionList()
                if (currentCollectionName == collectionName) {
                    refresh()
                }
            }
        }
    }

    fun remove(item: PicsDetails, collectionName: String) {
        remove(
            identifiers = listOfNotNull(
                item.url_to_original,
                item.url_to_video,
                item.lDownloadUrl()
            ) + (item.thumbnails?.mapNotNull { it.url } ?: emptyList()),
            collectionName = collectionName
        )
    }

    fun remove(url: String, collectionName: String) {
        remove(listOf(url), collectionName)
    }

    fun removeAll(items: List<PicsDetails>, collectionName: String) {
        val collectionRoot = File(AppPath.l_collection, collectionName)
        val removedCount = items
            .distinctBy { lPicsDetailsIdentityKey(it) }
            .count { item ->
                val folder = lFindCollectionItemFolder(collectionRoot, lCollectionItemIdentifiers(item))
                folder?.deleteRecursively() == true
            }

        if (removedCount > 0) {
            SnackBar.info("Удалено из коллекции: $removedCount")
            refreshCollectionList()
            if (currentCollectionName == collectionName) {
                refresh()
            }
        } else {
            SnackBar.error("Файлы не найдены")
        }
    }

    fun setManualCover(item: PicsDetails, collectionName: String? = currentCollectionName) {
        val name = collectionName ?: return
        val collectionRoot = File(AppPath.l_collection, name)
        val folder = lFindCollectionItemFolder(collectionRoot, lCollectionItemIdentifiers(item))
        if (folder == null) {
            SnackBar.error("Не удалось найти файл для обложки")
            return
        }

        val config = lReadCollectionConfig(collectionRoot).copy(coverFolderName = folder.name)
        lWriteCollectionConfig(collectionRoot, config)
        SnackBar.success("Обложка коллекции обновлена")
        refreshCollectionList()
    }

    fun removeDuplicateItems(collectionName: String? = currentCollectionName) {
        val name = collectionName ?: return
        val collectionRoot = File(AppPath.l_collection, name)
        val duplicateGroups = lFindCollectionDuplicateFolders(collectionRoot)
        val foldersToDelete = duplicateGroups.flatMap { group -> group.drop(1).map { it.second } }

        if (foldersToDelete.isEmpty()) {
            SnackBar.info("Дубли не найдены")
            refreshDuplicates(name)
            return
        }

        val removedCount = foldersToDelete.count { it.deleteRecursively() }
        SnackBar.info("Удалено дублей: $removedCount")
        refreshCollectionList()
        if (currentCollectionName == name) {
            refresh()
        }
    }

    fun refreshSmartCollectionCandidates() {
        scope.launch(Dispatchers.IO) {
            val candidates = lReadSmartCollectionCandidates()
            withContext(Dispatchers.Main) {
                smartCollectionCandidates.clear()
                smartCollectionCandidates.addAll(candidates)
            }
        }
    }

    fun createSmartCollection(candidate: LSmartCollectionCandidate) {
        scope.launch(Dispatchers.IO) {
            val result = lCreateSmartCollection(candidate)
            withContext(Dispatchers.Main) {
                result
                    .onSuccess { count ->
                        SnackBar.success("Создана коллекция ${candidate.collectionName}: $count")
                        refreshCollectionList()
                    }
                    .onFailure {
                        Timber.e(it, "SavedL_Collection createSmartCollection() error")
                        SnackBar.error("Ошибка создания smart-коллекции")
                    }
            }
        }
    }

    private fun remove(identifiers: List<String>, collectionName: String) {
        Timber.i("SavedL_Collection remove() identifiers:$identifiers collection:$collectionName")
        val collectionRoot = File(AppPath.l_collection, collectionName)
        val folder = lFindCollectionItemFolder(collectionRoot, identifiers)
        val file = identifiers.firstOrNull()?.lToFilePath()?.let { File(it) }

        val removed = when {
            folder != null -> folder.deleteRecursively()
            file != null && lIsInside(collectionRoot, file) && file.exists() -> file.delete()
            else -> false
        }

        if (removed) {
            SnackBar.info("Removed from collection")
            refreshCollectionList()
        } else {
            SnackBar.error("Файл не найден")
        }
        if (currentCollectionName == collectionName) {
            refresh()
        }
    }
}
