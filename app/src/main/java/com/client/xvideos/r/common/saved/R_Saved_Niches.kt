package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.NichesInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber
import kotlin.onSuccess

class R_Saved_Niches {

    val nichesDb = FileDB(AppPath.r_niches, "niches", NichesInfo::class.java)
    val list = nichesDb.list

    fun add(item: NichesInfo) {
        Timber.i("R_Saved_Niches add() id:${item.id} name:${item.name}")
        nichesDb.insert(item.id, item)
            .onSuccess {
                SnackBar.info("Группа добавлена")
                list.add(item)
            }
            .onFailure { e ->
                SnackBar.error("Ошибка добавления группы ${e.message}")
            }
    }

    fun remove(item: NichesInfo) {
        Timber.i("R_Saved_Niches remove() id:${item.id} name:${item.name}")
        nichesDb.delete(item.id)
            .onSuccess {
                SnackBar.info("Группа удалена")
                list.remove(item)
            }
            .onFailure { e -> SnackBar.error("Ошибка удаления группы ${e.message}") }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refresh() {
        nichesDb.refresh()
    }

}