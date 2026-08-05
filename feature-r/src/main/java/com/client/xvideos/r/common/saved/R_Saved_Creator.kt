package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.UserInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber
import kotlin.onSuccess

class R_Saved_Creator {

    val creatorDb = FileDB(AppPath.r_creators, "creator", UserInfo::class.java)

    var list = creatorDb.list

    fun add(item: UserInfo) {
        Timber.i("R_Saved_Creator add() id:${item.username}")
        creatorDb.insert(item.username, item)
            .onSuccess {
                SnackBar.success("Автор добавлен")
                list.add(item)
            }
            .onFailure { e ->
                SnackBar.error("Ошибка добавления Автора ${e.message}")
            }
    }

    fun remove(username: String) {
        Timber.i("R_Saved_Creator remove() id:${username}")
        creatorDb.delete(username)
            .onSuccess {
                SnackBar.info("Автор удален")
                //creatorsList.remove(item)
                refresh()
            }
            .onFailure { e -> SnackBar.error("Ошибка удаления Автора ${e.message}") }
    }

    fun updateIfSaved(item: UserInfo): Boolean {
        val index = list.indexOfFirst { it.username == item.username }
        if (index == -1) return false
        if (list[index] == item) return false

        return creatorDb.update(item.username, item)
            .onSuccess {
                Timber.i("R_Saved_Creator updateIfSaved() id:${item.username}")
                list[index] = item
            }
            .onFailure { e ->
                Timber.e(e, "R_Saved_Creator updateIfSaved() error id:${item.username}")
            }
            .isSuccess
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refresh() {
        creatorDb.refresh()
    }

}
