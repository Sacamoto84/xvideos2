package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.NichesInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class R_Saved_Niches(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    val nichesDb = FileDB(AppPath.r_niches, "niches", NichesInfo.serializer())
    val list = nichesDb.list

    fun add(item: NichesInfo) {
        Timber.i("R_Saved_Niches add() id:${item.id} name:${item.name}")
        scope.launch(Dispatchers.IO) {
            nichesDb.insert(item.id, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
                        list.add(item)
                    }
                    SnackBar.info("Группа добавлена")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления группы ${e.message}")
                }
        }
    }

    fun remove(item: NichesInfo) {
        Timber.i("R_Saved_Niches remove() id:${item.id} name:${item.name}")
        scope.launch(Dispatchers.IO) {
            nichesDb.delete(item.id)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
                    }
                    SnackBar.info("Группа удалена")
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления группы ${e.message}") }
        }
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            nichesDb.refresh()
        }
    }

}
