package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.model.sanitizeOrNull
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber
import kotlin.onSuccess

class R_Saved_Likes {

    val likesDb = FileDB(AppPath.r_likes, "likes", GifsInfo::class.java)

    var list = likesDb.list

    fun add(item: GifsInfo) {
        val safeItem = item.sanitizeOrNull() ?: run {
            SnackBar.error("Like add error: empty id")
            return
        }
        Timber.i("R_Saved_Likes add() id:${safeItem.id} userName:${safeItem.userName} url:${safeItem.urls.hd}")
        likesDb.insert(safeItem.id, safeItem)
            .onSuccess {
                SnackBar.success("Like")
                list.add(safeItem)
            }
            .onFailure { e ->
                SnackBar.error("Ошибка добавления лайка ${e.message}")
            }
    }

    fun remove(item: GifsInfo) {
        Timber.i("R_Saved_Likes remove() id:${item.id} userName:${item.userName} url:${item.urls.hd}")
        likesDb.delete(item.id)
            .onSuccess { SnackBar.info("Unlike") }
            .onFailure { e -> SnackBar.error("Ошибка удаления лайка ${e.message}") }
        refresh()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refresh() {
        likesDb.refresh()
        val sanitized = list.toList().sanitizeGifsInfoList()
        list.clear()
        list.addAll(sanitized)
    }

}
