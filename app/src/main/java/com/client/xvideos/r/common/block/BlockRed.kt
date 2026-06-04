package com.client.xvideos.r.common.block

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.GifsInfo
import com.redgifs.common.block.useCase.blockGetAllBlockedGifsInfo
import com.redgifs.common.block.useCase.blockItem as writeBlockedGif
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

    private val _blockList = MutableStateFlow<List<GifsInfo>>(emptyList())
    val blockList: StateFlow<List<GifsInfo>> get() = _blockList

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _blockList.value = withContext(Dispatchers.IO) {
                blockGetAllBlockedGifsInfo()
            }
        }
    }

    fun refreshListAndBlock(list: MutableStateFlow<List<GifsInfo>>) {
        val blockedSet = blockList.value.map { it.id }.toSet()
        list.value = list.value.filterNot { it.id in blockedSet }
    }

    fun blockItem(item: GifsInfo) {
        scope.launch {
            runCatching {
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
}
