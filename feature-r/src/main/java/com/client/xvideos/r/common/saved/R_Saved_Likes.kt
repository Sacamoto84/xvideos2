package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.model.sanitizeOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class R_Saved_Likes(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    val likesDb = FileDB(AppPath.r_likes, "likes", GifsInfo.serializer())

    val list = likesDb.list

    fun add(item: GifsInfo) {
        val safeItem = item.sanitizeOrNull() ?: run {
            SnackBar.error("Like add error: empty id")
            return
        }
        Timber.i(
            "R_Saved_Likes add() id:${safeItem.id} userName:${safeItem.userName} " +
                "url:${safeItem.urls.hd} -> ${likesDb.dirPath}/${safeItem.id}.likes"
        )
        scope.launch(Dispatchers.IO) {
            likesDb.insert(safeItem.id, safeItem)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == safeItem.id }
                        list.add(safeItem)
                    }
                    SnackBar.success("Like")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления лайка ${e.message}")
                }
        }
    }

    fun remove(item: GifsInfo) {
        Timber.i("R_Saved_Likes remove() id:${item.id} userName:${item.userName} url:${item.urls.hd}")
        scope.launch(Dispatchers.IO) {
            likesDb.delete(item.id)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
                    }
                    SnackBar.info("Unlike")
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления лайка ${e.message}") }
        }
    }

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            likesDb.refresh()
            val current = withContext(Dispatchers.Main) { list.toList() }
            val sanitized = current.sanitizeGifsInfoList()
            // Переписываем список только если санитизация реально что-то изменила,
            // иначе получаем лишнюю перезапись и мигание списка.
            if (sanitized != current) {
                withContext(Dispatchers.Main) {
                    list.replaceWith(sanitized)
                }
            }
        }
    }

}
