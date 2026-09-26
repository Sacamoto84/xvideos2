package com.client.xvideos.l.featured.saved

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.CollectionName
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.net.Luscious
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Тонкий holder состояния для раздела «Collection» в L.
 *
 * Файловая система выведена в [LCollectionFs.lReadCollections] / [lReadCollectionItems] /
 * [lFindCollectionItemFolder], сетевая часть — в [lPersistPicsDetailsToFolder].
 * Этот класс держит только public API + Compose state и оркестрирует вызовы.
 *
 * @param scope Область корутин для выполнения дисковых и сетевых операций.
 * @param luscious Ссылка на сервис API Luscious.
 */
class SavedL_Collection(
    private val scope: CoroutineScope,
    private val luscious: Luscious
) {

    /** Список элементов картинок текущей открытой коллекции. */
    val listUrl = mutableStateListOf<PicsDetails>()
    /** Список всех существующих локальных коллекций с метаданными. */
    val collectionList = mutableStateListOf<LCollectionEntity>()
    /** Список групп дубликатов в текущей коллекции. */
    val duplicateGroups = mutableStateListOf<LCollectionDuplicateGroup>()

    val isCollectionsEmpty: Boolean get() = collectionList.isEmpty()
    val isCollectionsNotEmpty: Boolean get() = collectionList.isNotEmpty()
    val collectionsCount: Int get() = collectionList.size

    val isCurrentItemsEmpty: Boolean get() = listUrl.isEmpty()
    val isCurrentItemsNotEmpty: Boolean get() = listUrl.isNotEmpty()
    val currentItemsCount: Int get() = listUrl.size

    fun containsCollection(name: String?): Boolean =
        if (name.isNullOrBlank()) false else collectionList.any { it.collection.equals(name, ignoreCase = true) }

    fun findCollectionByNameOrNull(name: String?): LCollectionEntity? =
        if (name.isNullOrBlank()) null else collectionList.firstOrNull { it.collection.equals(name, ignoreCase = true) }

    private val progress = LDownloadProgress(scope)
    /** Поток совокупного процента скачивания файлов при добавлении в коллекцию. */
    val percentDownload: StateFlow<Float> = progress.percentDownload

    /** Имя текущей выбранной/открытой коллекции (null, если открыт список коллекций). */
    var currentCollectionName by mutableStateOf<String?>(null)
    /** Текущий порядок сортировки коллекций. */
    var sortOrder by mutableStateOf(LCollectionSortOrder.RECENT)

    //----- Dialogs -----
    /** Флаг видимости диалога выбора коллекции для добавления элемента(ов). */
    var visibleDialog by mutableStateOf(false)
    /** Флаг видимости диалога создания новой коллекции. */
    var visibleDialogCreateNew by mutableStateOf(false)
    /** Выбранный элемент медиа для добавления через диалог. */
    var collectionItemGifInfo by mutableStateOf<PicsDetails?>(null)
    /** Список элементов, ожидающих добавления в выбранную коллекцию. */
    val collectionItemsPendingAdd = mutableStateListOf<PicsDetails>()
    //-------------------

    init {
        refreshCollectionList()
    }

    /* ---------- Список коллекций ---------- */

    private var refreshCollectionJob: Job? = null

    /**
     * Асинхронно сканирует директорию коллекций на IO-потоке и обновляет [collectionList].
     */
    fun refreshCollectionList() {
        // Обход каталога коллекций (с подсчётом элементов/дублей и чтением
        // metadata.json) — на IO; обновление Compose-state — на Main, иначе ANR.
        // Отменяем незавершённый скан при повторном запуске.
        val order = sortOrder
        refreshCollectionJob?.cancel()
        refreshCollectionJob = scope.launch(Dispatchers.IO) {
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
                collectionList.replaceWith(items)
                Timber.i("SavedL_Collection refreshCollectionList() collections:${items.size}")
            }
        }
    }

    /**
     * Применяет новый порядок сортировки коллекций и перезагружает их список.
     *
     * @param order Новый порядок сортировки [LCollectionSortOrder].
     */
    fun applySortOrder(order: LCollectionSortOrder) {
        sortOrder = order
        refreshCollectionList()
    }

    /**
     * Создает новую локальную коллекцию с именем [collectionName].
     *
     * @param collectionName Желаемое имя коллекции.
     */
    fun createCollection(collectionName: String) {
        Timber.i("SavedL_Collection createCollection() collectionName:$collectionName")
        val safeName = CollectionName.normalizeOrNull(collectionName)
        if (safeName == null) {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        scope.launch(Dispatchers.IO) {
            val collectionRoot = File(AppPath.l_collection, safeName)
            if (collectionRoot.exists()) {
                withContext(Dispatchers.Main) {
                    SnackBar.error("Коллекция уже существует")
                }
                return@launch
            }
            val created = collectionRoot.mkdirs() || collectionRoot.isDirectory
            withContext(Dispatchers.Main) {
                if (created) {
                    SnackBar.success("Коллекция $safeName создана")
                    refreshCollectionList()
                } else {
                    SnackBar.error("Не удалось создать папку коллекции $safeName")
                }
            }
        }
    }

    /**
     * Удаляет коллекцию [collectionName] и все входящие в неё файлы.
     *
     * @param collectionName Название удаляемой коллекции.
     */
    fun deleteCollection(collectionName: String) {
        Timber.i("SavedL_Collection deleteCollection() collectionName:$collectionName")
        // Имя приходит из списка на экране, но список строится по содержимому
        // каталога: папка с «плохим» именем, созданная старой сборкой или
        // приехавшая по P2P, дала бы deleteRecursively() за пределами корня.
        val safeName = CollectionName.normalizeOrNull(collectionName)
        if (safeName == null) {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        // deleteRecursively по всей коллекции — это тысячи файлов, только на IO.
        scope.launch(Dispatchers.IO) {
            val deleted = File(AppPath.l_collection, safeName).deleteRecursively()
            withContext(Dispatchers.Main) {
                if (deleted) {
                    collectionCache.remove(safeName)
                    if (currentCollectionName == safeName) {
                        currentCollectionName = null
                        listUrl.clear()
                    }
                    SnackBar.success("Коллекция $safeName удалена")
                    refreshCollectionList()
                } else {
                    SnackBar.error("Ошибка удаления коллекции $safeName")
                }
            }
        }
    }

    /**
     * Переименовывает коллекцию из [oldName] в [newName].
     *
     * @param oldName Текущее имя коллекции.
     * @param newName Новое имя коллекции.
     */
    fun renameCollection(oldName: String, newName: String) {
        Timber.i("SavedL_Collection renameCollection() oldName:$oldName newName:$newName")
        val trimmedNewName = CollectionName.normalizeOrNull(newName)
        if (trimmedNewName == null) {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        val safeOldName = CollectionName.normalizeOrNull(oldName)
        if (safeOldName == null) {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        if (safeOldName == trimmedNewName) {
            return
        }

        // Переименование и fallback copyRecursively — это IO-операции на файловой системе
        scope.launch(Dispatchers.IO) {
            val oldRoot = File(AppPath.l_collection, safeOldName)
            val newRoot = File(AppPath.l_collection, trimmedNewName)
            if (!oldRoot.exists()) {
                withContext(Dispatchers.Main) {
                    SnackBar.error("Коллекция не найдена")
                }
                return@launch
            }
            if (newRoot.exists()) {
                withContext(Dispatchers.Main) {
                    SnackBar.error("Коллекция уже существует")
                }
                return@launch
            }

            val renamed = if (!oldRoot.renameTo(newRoot)) {
                try {
                    val copied = oldRoot.copyRecursively(newRoot, overwrite = false)
                    if (copied) {
                        oldRoot.deleteRecursively()
                        true
                    } else {
                        newRoot.deleteRecursively()
                        false
                    }
                } catch (e: Exception) {
                    Timber.e(e, "SavedL_Collection renameCollection() fallback failed")
                    newRoot.deleteRecursively()
                    false
                }
            } else {
                true
            }

            withContext(Dispatchers.Main) {
                if (renamed) {
                    collectionCache.remove(safeOldName)?.let { oldFlow ->
                        collectionCache[trimmedNewName] = oldFlow
                        reloadCollectionItems(trimmedNewName, oldFlow)
                    }
                    if (currentCollectionName == safeOldName) {
                        currentCollectionName = trimmedNewName
                        refresh()
                    }
                    refreshCollectionList()
                    SnackBar.success("Коллекция переименована")
                } else {
                    SnackBar.error("Ошибка переименования коллекции")
                }
            }
        }
    }

    /* ---------- Текущая коллекция и кэш сессии ---------- */

    private val collectionCache = ConcurrentHashMap<String, MutableStateFlow<List<PicsDetails>?>>()

    /**
     * Возвращает реактивный поток элементов коллекции [collectionName], кэшируемый в памяти сессии.
     *
     * @param collectionName Название запрашиваемой коллекции.
     */
    fun getCollectionItems(collectionName: String): StateFlow<List<PicsDetails>?> {
        val safeName = CollectionName.normalizeOrNull(collectionName)
            ?: return MutableStateFlow(emptyList())
        return collectionCache.getOrPut(safeName) {
            MutableStateFlow<List<PicsDetails>?>(null).also { flow ->
                reloadCollectionItems(safeName, flow)
            }
        }
    }

    /**
     * Сбрасывает кэш элементов указанной коллекции и перезагружает её с диска.
     *
     * @param collectionName Название обновляемой коллекции.
     */
    fun invalidateCollection(collectionName: String) {
        val safeName = CollectionName.normalizeOrNull(collectionName) ?: return
        collectionCache[safeName]?.let { flow ->
            reloadCollectionItems(safeName, flow)
        }
    }

    private fun reloadCollectionItems(
        safeName: String,
        flow: MutableStateFlow<List<PicsDetails>?>
    ) {
        scope.launch(Dispatchers.IO) {
            Timber.i("SavedL_Collection reloadCollectionItems() collection:$safeName")
            val items = try {
                lReadCollectionItems(File(AppPath.l_collection, safeName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SavedL_Collection reloadCollectionItems() Ошибка получения списка коллекции")
                emptyList()
            }
            flow.value = items
            withContext(Dispatchers.Main) {
                if (currentCollectionName == safeName) {
                    listUrl.replaceWith(items)
                }
            }
        }
    }

    private var refreshDuplicatesJob: Job? = null
    private var mutationJob: Job? = null

    /**
     * Устанавливает коллекцию [collectionName] в качестве текущей открытой.
     *
     * @param collectionName Имя открываемой коллекции.
     */
    fun setCollection(collectionName: String) {
        val safeName = CollectionName.normalizeOrNull(collectionName) ?: run {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        currentCollectionName = safeName
        val cached = collectionCache[safeName]?.value
        listUrl.replaceWith(cached ?: emptyList())
        if (cached == null) {
            refresh()
        }
    }

    /**
     * Сбрасывает текущую выбранную коллекцию и очищает [listUrl].
     */
    fun exitCollection() {
        currentCollectionName = null
        listUrl.clear()
    }

    /**
     * Перезагружает элементы текущей открытой коллекции [currentCollectionName].
     */
    fun refresh() {
        val rawName = currentCollectionName ?: return
        val collectionName = CollectionName.normalizeOrNull(rawName) ?: run {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        val flow = collectionCache.getOrPut(collectionName) { MutableStateFlow(null) }
        reloadCollectionItems(collectionName, flow)
    }

    /**
     * Пересчитывает группы дубликатов элементов для указанной коллекции.
     *
     * @param collectionName Название коллекции (по умолчанию текущая).
     */
    fun refreshDuplicates(collectionName: String? = currentCollectionName) {
        val rawName = collectionName ?: return
        val name = CollectionName.normalizeOrNull(rawName) ?: return
        refreshDuplicatesJob?.cancel()
        refreshDuplicatesJob = scope.launch(Dispatchers.IO) {
            val groups = lReadCollectionDuplicateGroups(File(AppPath.l_collection, name))
            withContext(Dispatchers.Main) {
                duplicateGroups.replaceWith(groups)
            }
        }
    }

    /* ---------- Элементы ---------- */

    /**
     * Начинает процесс добавления одиночного элемента [item] в коллекцию через диалог.
     */
    fun beginAddToCollection(item: PicsDetails) {
        collectionItemsPendingAdd.clear()
        collectionItemsPendingAdd.add(item)
        collectionItemGifInfo = item
        visibleDialog = true
    }

    /**
     * Начинает процесс пакетного добавления элементов [items] в коллекцию через диалог.
     */
    fun beginAddManyToCollection(items: List<PicsDetails>) {
        if (items.isEmpty()) return
        val unique = if (items.size == 1) items else items.distinctBy { lPicsDetailsIdentityKey(it) }
        collectionItemsPendingAdd.replaceWith(unique)
        collectionItemGifInfo = collectionItemsPendingAdd.firstOrNull()
        visibleDialog = collectionItemsPendingAdd.isNotEmpty()
    }

    /**
     * Завершает диалог добавления, сохраняя ожидающие элементы в коллекцию [collectionName].
     */
    fun addPendingToCollection(collectionName: String) {
        val items = collectionItemsPendingAdd.toList()
            .ifEmpty { listOfNotNull(collectionItemGifInfo) }
        addAll(items, collectionName)
        visibleDialog = false
        collectionItemsPendingAdd.clear()
        collectionItemGifInfo = null
    }

    /**
     * Скачивает и сохраняет медиафайл [item] в указанную коллекцию [collectionName].
     */
    fun add(item: PicsDetails, collectionName: String) {
        addAll(listOf(item), collectionName)
    }

    /**
     * Пакетно скачивает и сохраняет медиафайлы [items] в коллекцию [collectionName].
     */
    fun addAll(items: List<PicsDetails>, collectionName: String) {
        if (items.isEmpty()) return
        val safeName = CollectionName.normalizeOrNull(collectionName) ?: run {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        val uniqueItems = if (items.size == 1) items else items.distinctBy { lPicsDetailsIdentityKey(it) }
        if (uniqueItems.isEmpty()) return

        Timber.i("SavedL_Collection addAll() count:${uniqueItems.size} collection:$safeName")

        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            var successCount = 0
            var errorCount = 0

            uniqueItems.forEach { item ->
                lPersistPicsDetailsToFolder(
                    item = item,
                    root = File(AppPath.l_collection, safeName),
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
                invalidateCollection(safeName)
                if (currentCollectionName == safeName) {
                    refresh()
                }
            }
        }
    }

    /**
     * Удаляет медиафайл [item] из коллекции [collectionName].
     */
    fun remove(item: PicsDetails, collectionName: String) {
        val identifiers = ArrayList<String>(4)
        item.url_to_original?.let { if (it.isNotEmpty()) identifiers.add(it) }
        item.url_to_video?.let { if (it.isNotEmpty()) identifiers.add(it) }
        item.lDownloadUrl()?.let { if (it.isNotEmpty()) identifiers.add(it) }
        item.thumbnails?.forEach { thumb ->
            thumb.url?.let { if (it.isNotEmpty()) identifiers.add(it) }
        }
        if (identifiers.isEmpty()) {
            SnackBar.error("Файл не найден")
            return
        }
        remove(identifiers = identifiers, collectionName = collectionName)
    }

    /**
     * Удаляет медиафайл по URL/пути [url] из коллекции [collectionName].
     */
    fun remove(url: String, collectionName: String) {
        if (url.isBlank()) return
        remove(listOf(url), collectionName)
    }

    /**
     * Пакетно удаляет медиафайлы [items] из коллекции [collectionName].
     */
    fun removeAll(items: List<PicsDetails>, collectionName: String) {
        if (items.isEmpty()) return
        val safeName = CollectionName.normalizeOrNull(collectionName) ?: run {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        val uniqueItems = if (items.size == 1) items else items.distinctBy { lPicsDetailsIdentityKey(it) }
        if (uniqueItems.isEmpty()) return
        // Обход папок коллекции на каждый элемент плюс рекурсивное удаление — на IO.
        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            val collectionRoot = File(AppPath.l_collection, safeName)
            val removedCount = uniqueItems.count { item ->
                val folder = lFindCollectionItemFolder(collectionRoot, lCollectionItemIdentifiers(item))
                folder?.deleteRecursively() == true
            }

            if (removedCount > 0) {
                SnackBar.info("Удалено из коллекции: $removedCount")
                refreshCollectionList()
                invalidateCollection(safeName)
                if (currentCollectionName == safeName) {
                    refresh()
                }
            } else {
                SnackBar.error("Файлы не найдены")
            }
        }
    }

    /**
     * Назначает элемент [item] постоянной пользовательской обложкой для коллекции [collectionName].
     */
    fun setManualCover(item: PicsDetails, collectionName: String? = currentCollectionName) {
        val rawName = collectionName ?: return
        val name = CollectionName.normalizeOrNull(rawName) ?: run {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        // Поиск папки элемента + чтение и запись config-файла — файловые операции.
        // Вызов идёт из onClick меню, с UI-потока это фриз (см. refreshCollectionList).
        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            val collectionRoot = File(AppPath.l_collection, name)
            try {
                val folder = lFindCollectionItemFolder(collectionRoot, lCollectionItemIdentifiers(item))
                if (folder == null) {
                    SnackBar.error("Не удалось найти файл для обложки")
                    return@launch
                }

                val config = lReadCollectionConfig(collectionRoot).copy(coverFolderName = folder.name)
                lWriteCollectionConfig(collectionRoot, config)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SavedL_Collection setManualCover() Ошибка установки обложки")
                SnackBar.error("Ошибка установки обложки")
                return@launch
            }
            SnackBar.success("Обложка коллекции обновлена")
            refreshCollectionList()
        }
    }

    /**
     * Находит и удаляет повторяющиеся дубликаты элементов внутри коллекции [collectionName], оставляя только одну копию каждого.
     */
    fun removeDuplicateItems(collectionName: String? = currentCollectionName) {
        val rawName = collectionName ?: return
        val name = CollectionName.normalizeOrNull(rawName) ?: run {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        // Обход всех папок коллекции с чтением metadata.json каждого элемента плюс
        // рекурсивное удаление дублей — только на IO, иначе ANR на большой коллекции.
        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            val collectionRoot = File(AppPath.l_collection, name)
            val removedCount = try {
                val groups = lFindCollectionDuplicateFolders(collectionRoot)
                val foldersToDelete = groups.flatMap { group -> group.drop(1).map { it.second } }

                if (foldersToDelete.isEmpty()) {
                    SnackBar.info("Дубли не найдены")
                    refreshDuplicates(name)
                    return@launch
                }

                foldersToDelete.count { it.deleteRecursively() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SavedL_Collection removeDuplicateItems() Ошибка удаления дублей")
                SnackBar.error("Ошибка удаления дублей")
                return@launch
            }

            SnackBar.info("Удалено дублей: $removedCount")
            refreshCollectionList()
            invalidateCollection(name)
            if (currentCollectionName == name) {
                refresh()
            }
        }
    }


    private fun remove(identifiers: List<String>, collectionName: String) {
        if (identifiers.isEmpty()) return
        val safeName = CollectionName.normalizeOrNull(collectionName) ?: run {
            SnackBar.error("Недопустимое название коллекции")
            return
        }
        Timber.i("SavedL_Collection remove() identifiers:$identifiers collection:$safeName")
        // Поиск папки элемента обходит коллекцию, удаление рекурсивное — на IO.
        scope.launch(Dispatchers.IO) {
            val collectionRoot = File(AppPath.l_collection, safeName)
            val folder = lFindCollectionItemFolder(collectionRoot, identifiers)
            val file = identifiers.firstOrNull()?.lToFilePath()?.let { File(it) }

            val removed = when {
                folder != null -> folder.deleteRecursively()
                file != null && lIsInside(collectionRoot, file) && file.exists() -> file.delete()
                else -> false
            }

            if (removed) {
                SnackBar.info("Удалено из коллекции")
                refreshCollectionList()
                invalidateCollection(safeName)
            } else {
                SnackBar.error("Файл не найден")
            }
            if (currentCollectionName == safeName) {
                refresh()
            }
        }
    }
}
