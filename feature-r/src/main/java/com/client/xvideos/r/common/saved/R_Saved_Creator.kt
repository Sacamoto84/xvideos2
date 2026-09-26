package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Хранилище избранных создателей контента (авторов) RedGifs на базе [FileDB].
 *
 * Файлы метаданных [UserInfo] сохраняются в `AppPath.r_creators` с расширением `.creator`.
 *
 * @param scope Корутин-скоп для фонового I/O.
 */
class R_Saved_Creator(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    /** Файловая БД авторов. */
    val creatorDb = FileDB(AppPath.r_creators, "creator", UserInfo.serializer())

    /** Реактивный список избранных авторов для Compose UI. */
    val list = creatorDb.list

    /**
     * Сохраняет автора [item] в избранные на диске и в памяти.
     */
    fun add(item: UserInfo) {
        Timber.i("R_Saved_Creator add() id:${item.username}")
        scope.launch(Dispatchers.IO) {
            creatorDb.insert(item.username, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.username == item.username }
                        if (existingIndex == list.lastIndex && list[existingIndex] == item) {
                            return@withContext
                        }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
                        list.add(item)
                    }
                    SnackBar.success("Автор добавлен")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления Автора ${e.message}")
                }
        }
    }

    /**
     * Удаляет автора по [username] из избранных на диске и в памяти.
     */
    fun remove(username: String) {
        Timber.i("R_Saved_Creator remove() id:${username}")
        scope.launch(Dispatchers.IO) {
            creatorDb.delete(username)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.username == username }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
                    }
                    SnackBar.info("Автор удален")
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления Автора ${e.message}") }
        }
    }

    /**
     * Если автор уже сохранен в избранном, обновляет его данные актуальной версией [item].
     * Возвращает true, если автор был в списке и обновление запущено.
     */
    fun updateIfSaved(item: UserInfo): Boolean {
        val index = list.indexOfFirst { it.username == item.username }
        if (index == -1) return false
        if (list[index] == item) return false

        scope.launch(Dispatchers.IO) {
            creatorDb.update(item.username, item)
                .onSuccess {
                    Timber.i("R_Saved_Creator updateIfSaved() id:${item.username}")
                    withContext(Dispatchers.Main) {
                        val idx = if (index < list.size && list[index].username == item.username) {
                            index
                        } else {
                            list.indexOfFirst { it.username == item.username }
                        }
                        if (idx != -1) {
                            list[idx] = item
                        }
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "R_Saved_Creator updateIfSaved() error id:${item.username}")
                }
        }
        return true
    }

    /** Проверяет, пуст ли список избранных авторов. */
    val isEmpty: Boolean get() = list.isEmpty()

    /** Проверяет, есть ли хотя бы один избранный автор. */
    val isNotEmpty: Boolean get() = list.isNotEmpty()

    /** Количество избранных авторов. */
    val count: Int get() = list.size

    /** Проверяет наличие автора по никнейму [username]. */
    fun contains(username: String): Boolean = username.isNotBlank() && list.any { it.username == username }

    /** Проверяет наличие автора [item] в избранном. */
    fun contains(item: UserInfo?): Boolean = item != null && contains(item.username)

    /** Поиск автора в избранном по [username]. */
    fun findByUsernameOrNull(username: String?): UserInfo? =
        if (username.isNullOrBlank()) null else list.firstOrNull { it.username == username }

    private var refreshJob: Job? = null

    /**
     * Перечитывает список сохраненных авторов с диска.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            creatorDb.refresh()
        }
    }

}
