package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateSetOf
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.x.model.ItemsX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.onSuccess

/**
 * Хранилище избранных видеороликов раздела X.
 *
 * Сохраняет объекты [ItemsX] в файловой БД на диске ([FileDB] в [AppPath.x_favorites]),
 * поддерживает реактивный snapshot-список [list] и множество [favoriteIds]
 * для мгновенной O(1)-проверки [contains].
 *
 * @property scope CoroutineScope для выполнения операций дискового ввода-вывода на [Dispatchers.IO].
 */
@Stable
class SavedX_Favorites(val scope: CoroutineScope) {

    private val favoritesDb = FileDB(AppPath.x_favorites, "ItemsX", ItemsX.serializer())

    /** Наблюдаемый список избранных элементов для Compose UI. */
    val list = favoritesDb.list

    /**
     * Множество id избранного для O(1)-проверки [contains].
     * Snapshot-set — изменения наблюдаются Compose, поэтому индикаторы корректно перерисовываются.
     */
    val favoriteIds = mutableStateSetOf<Long>()

    init {
        refresh()
    }

    /**
     * Добавляет ролик в избранное.
     *
     * Файловый I/O вынесен на [Dispatchers.IO], обновление списка и множества [favoriteIds]
     * выполняется на [Dispatchers.Main].
     *
     * @param item Объект добавляемого ролика.
     */
    fun add(item: ItemsX) {
        if (item.id <= 0L) {
            SnackBar.error("Недопустимый ID видео")
            return
        }
        scope.launch(Dispatchers.IO) {
            favoritesDb.insert(item.id.toString(), item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.id == item.id }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
                        list.add(item)
                        favoriteIds.add(item.id)
                    }
                    SnackBar.info("Добавлено в избранное")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления ${e.message}")
                }
        }
    }

    /**
     * Удаляет ролик из избранного.
     *
     * @param item Объект удаляемого ролика.
     */
    fun remove(item: ItemsX) {
        if (item.id <= 0L) {
            SnackBar.error("Недопустимый ID видео")
            return
        }
        scope.launch(Dispatchers.IO) {
            favoritesDb.delete(item.id.toString())
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.id == item.id }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
                        favoriteIds.remove(item.id)
                    }
                    SnackBar.info("Удалён из избранного")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка удаления ${e.message}")
                }
        }
    }

    /**
     * Быстрая O(1)-проверка нахождения ролика в избранном.
     *
     * @param id Числовой ID видео.
     * @return `true`, если ролик находится в избранном.
     */
    fun contains(id: Long): Boolean = id > 0L && favoriteIds.contains(id)

    private var refreshJob: Job? = null

    /**
     * Перечитывает записи из локального файлового хранилища в память и обновляет кэш идентификаторов.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            favoritesDb.refresh()
            withContext(Dispatchers.Main) {
                favoriteIds.clear()
                for (item in list) {
                    favoriteIds.add(item.id)
                }
            }
        }
    }
}
