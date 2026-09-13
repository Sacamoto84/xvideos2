package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.mutableStateSetOf
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.x.model.ItemsX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.onSuccess

class SavedX_Favorites(val scope: CoroutineScope) {

    private val favoritesDb = FileDB(AppPath.x_favorites, "ItemsX", ItemsX.serializer())

    val list = favoritesDb.list

    /**
     * Множество id избранного для O(1)-проверки [contains].
     * Snapshot-set — изменения наблюдаются Compose, поэтому индикаторы корректно перерисовываются.
     */
    val favoriteIds = mutableStateSetOf<Long>()

    init {
        refresh()
    }

    /** Файловый I/O вынесен на [Dispatchers.IO]: раньше запись шла в главном потоке (риск ANR). */
    fun add(item: ItemsX) {
        scope.launch(Dispatchers.IO) {
            favoritesDb.insert(item.id.toString(), item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
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

    fun remove(item: ItemsX) {
        scope.launch(Dispatchers.IO) {
            favoritesDb.delete(item.id.toString())
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
                        favoriteIds.remove(item.id)
                    }
                    SnackBar.info("Удалён из избранного")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка удаления ${e.message}")
                }
        }
    }

    /** Быстрая O(1)-проверка принадлежности к избранному. */
    fun contains(id: Long): Boolean = favoriteIds.contains(id)

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            favoritesDb.refresh()
            val ids = list.map { it.id }
            withContext(Dispatchers.Main) {
                favoriteIds.clear()
                favoriteIds.addAll(ids)
            }
        }
    }
}
