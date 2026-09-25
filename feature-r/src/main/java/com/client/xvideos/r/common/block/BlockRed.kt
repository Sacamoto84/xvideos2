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

@Singleton
class BlockRed @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) {
    var blockItem: GifsInfo? = null
    var blockVisibleDialog by mutableStateOf(false)

    val blockList: StateFlow<List<GifsInfo>>
        field = MutableStateFlow<List<GifsInfo>>(emptyList())

    val blockedIds: StateFlow<Set<String>>
        field = MutableStateFlow<Set<String>>(emptySet())

    init {
        refresh()
    }

    fun refresh(): kotlinx.coroutines.Job = scope.launch {
        val blocked = withContext(Dispatchers.IO) {
            blockGetAllBlockedGifsInfo()
        }
        blockList.value = blocked
        blockedIds.value = blocked.mapTo(HashSet(blocked.size)) { it.id }
    }

    fun isBlocked(id: String): Boolean = id in blockedIds.value

    fun refreshListAndBlock(list: MutableStateFlow<List<GifsInfo>>) {
        val blocked = blockedIds.value
        if (blocked.isEmpty()) return
        val current = list.value
        if (current.any { it.id in blocked }) {
            list.value = current.filterNot { it.id in blocked }
        }
    }

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
