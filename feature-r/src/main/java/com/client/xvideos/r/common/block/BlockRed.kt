package com.client.xvideos.r.common.block

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.common.block.useCase.blockGetAllBlockedGifsInfo
import com.client.xvideos.r.common.block.useCase.blockItem as writeBlockedGif
import com.client.xvideos.r.common.block.useCase.unblockItem as removeBlockedGif
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Синглтон управления блокировками контента и авторов в модуле RedGifs.
 *
 * Отвечает за:
 * - Хранение и реактивную публикацию списка заблокированных гифок ([blockList]) и их id ([blockedIds]);
 * - Персистентную запись файлов блокировок на диск в `AppPath.r_block`;
 * - Фильтрацию лент и проверку элементов на статус блокировки ([isBlocked], [isBlockedUser]);
 * - Управление состоянием диалога подтверждения блокировки в UI ([blockVisibleDialog]).
 *
 * @param scope Корутин-скоп уровня приложения.
 */
@Singleton
class BlockRed @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) {
    /** Текущий элемент [GifsInfo], выбранный для блокировки через диалог. */
    var blockItem: GifsInfo? = null

    /** Флаг видимости диалога подтверждения блокировки. */
    var blockVisibleDialog by mutableStateOf(false)

    /** Реактивный поток полного списка всех заблокированных элементов [GifsInfo]. */
    val blockList: StateFlow<List<GifsInfo>>
        field = MutableStateFlow<List<GifsInfo>>(emptyList())

    /** Реактивный набор уникальных строковых идентификаторов заблокированных гифок. */
    val blockedIds: StateFlow<Set<String>>
        field = MutableStateFlow<Set<String>>(emptySet())

    init {
        refresh()
    }

    /**
     * Асинхронно перечитывает все файлы блокировок с диска и обновляет [blockList] и [blockedIds].
     */
    fun refresh(): kotlinx.coroutines.Job = scope.launch {
        val blocked = withContext(Dispatchers.IO) {
            blockGetAllBlockedGifsInfo()
        }
        blockList.value = blocked
        blockedIds.value = blocked.mapTo(HashSet(blocked.size)) { it.id }
    }

    /** Общее количество заблокированных элементов. */
    val blockedCount: Int get() = blockedIds.value.size

    /** Проверяет наличие хотя бы одной заблокированной гифки. */
    val hasBlocked: Boolean get() = blockedIds.value.isNotEmpty()

    /** Проверяет, пуст ли список блокировок. */
    val isEmpty: Boolean get() = blockedIds.value.isEmpty()

    /**
     * Проверяет, заблокирована ли гифка с указанным [id].
     */
    fun isBlocked(id: String): Boolean = id.isNotEmpty() && id in blockedIds.value

    /**
     * Проверяет, заблокирован ли переданный элемент [item].
     */
    fun isBlocked(item: GifsInfo?): Boolean = item != null && isBlocked(item.id)

    /**
     * Проверяет, есть ли блокировки у автора с указанным [userName] (без учета регистра).
     */
    fun isBlockedUser(userName: String): Boolean =
        userName.isNotEmpty() && blockList.value.any { it.userName.equals(userName, ignoreCase = true) }

    /**
     * Фильтрует переданный список [list], удаляя из него элементы, находящиеся в [blockedIds].
     */
    fun refreshListAndBlock(list: MutableStateFlow<List<GifsInfo>>) {
        val blocked = blockedIds.value
        if (blocked.isEmpty()) return
        val current = list.value
        if (current.any { it.id in blocked }) {
            list.value = current.filterNot { it.id in blocked }
        }
    }

    /**
     * Блокирует переданный элемент [item]:
     * записывает файл `.block` на диск и обновляет состояние [refresh].
     */
    fun blockItem(item: GifsInfo) {
        scope.launch {
            runCatchingCancellable {
                withContext(Dispatchers.IO) {
                    writeBlockedGif(item).getOrThrow()
                }
                refresh()
            }.onSuccess {
                SnackBar.success("GIFs заблокирован")
            }.onFailure { error ->
                Timber.e(error, "!!! Не удалось заблокировать GIF")
                SnackBar.error("Ошибка блокировки: ${error.message}")
            }
        }
    }

    /**
     * Разблокирует переданный элемент [item]:
     * удаляет файл `.block` с диска и обновляет состояние [refresh].
     */
    fun unblockItem(item: GifsInfo) {
        scope.launch {
            runCatchingCancellable {
                withContext(Dispatchers.IO) {
                    removeBlockedGif(item).getOrThrow()
                }
                refresh()
            }.onSuccess {
                SnackBar.success("GIF разблокирован")
            }.onFailure { error ->
                Timber.e(error, "!!! Не удалось разблокировать GIF")
                SnackBar.error("Ошибка разблокировки: ${error.message}")
            }
        }
    }
}
