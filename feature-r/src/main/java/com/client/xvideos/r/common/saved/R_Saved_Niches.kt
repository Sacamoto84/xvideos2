package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.NichesInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Хранилище избранных ниш пользователя в RedGifs на базе [FileDB].
 *
 * Файлы метаданных [NichesInfo] сохраняются в `AppPath.r_niches` с расширением `.niches`.
 *
 * @param scope Корутин-скоп для асинхронных операций.
 */
class R_Saved_Niches(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    /** Файловая БД избранных ниш. */
    val nichesDb = FileDB(AppPath.r_niches, "niches", NichesInfo.serializer())
    /** Реактивный список избранных ниш для Compose UI. */
    val list = nichesDb.list

    /**
     * Добавляет нишу [item] в избранное на диске и в памяти.
     */
    fun add(item: NichesInfo) {
        Timber.i("R_Saved_Niches add() id:${item.id} name:${item.name}")
        scope.launch(Dispatchers.IO) {
            nichesDb.insert(item.id, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.id == item.id }
                        if (existingIndex == list.lastIndex && list[existingIndex] == item) {
                            return@withContext
                        }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
                        list.add(item)
                    }
                    SnackBar.info("Группа добавлена")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления группы ${e.message}")
                }
        }
    }

    /**
     * Удаляет нишу [item] из избранного на диске и в памяти.
     */
    fun remove(item: NichesInfo) {
        Timber.i("R_Saved_Niches remove() id:${item.id} name:${item.name}")
        scope.launch(Dispatchers.IO) {
            nichesDb.delete(item.id)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.id == item.id }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
                    }
                    SnackBar.info("Группа удалена")
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления группы ${e.message}") }
        }
    }

    /** Проверяет, пуст ли список избранных ниш. */
    val isEmpty: Boolean get() = list.isEmpty()

    /** Проверяет, есть ли хотя бы одна сохраненная ниша. */
    val isNotEmpty: Boolean get() = list.isNotEmpty()

    /** Количество сохраненных ниш. */
    val count: Int get() = list.size

    /** Быстрая проверка принадлежности ниши по ее [id]. */
    fun contains(id: String): Boolean = id.isNotBlank() && list.any { it.id == id }

    /** Проверка наличия объекта ниши [item] в избранном. */
    fun contains(item: NichesInfo?): Boolean = item != null && contains(item.id)

    /** Поиск ниши в избранном по ее [id]. */
    fun findByIdOrNull(id: String?): NichesInfo? =
        if (id.isNullOrBlank()) null else list.firstOrNull { it.id == id }

    private var refreshJob: Job? = null

    /**
     * Перечитывает список сохраненных ниш с диска.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            nichesDb.refresh()
        }
    }

}
