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

class R_Saved_Creator(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    val creatorDb = FileDB(AppPath.r_creators, "creator", UserInfo.serializer())

    val list = creatorDb.list

    fun add(item: UserInfo) {
        Timber.i("R_Saved_Creator add() id:${item.username}")
        scope.launch(Dispatchers.IO) {
            creatorDb.insert(item.username, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.username == item.username }
                        list.add(item)
                    }
                    SnackBar.success("Автор добавлен")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления Автора ${e.message}")
                }
        }
    }

    fun remove(username: String) {
        Timber.i("R_Saved_Creator remove() id:${username}")
        scope.launch(Dispatchers.IO) {
            creatorDb.delete(username)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.username == username }
                    }
                    SnackBar.info("Автор удален")
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления Автора ${e.message}") }
        }
    }

    fun updateIfSaved(item: UserInfo): Boolean {
        val index = list.indexOfFirst { it.username == item.username }
        if (index == -1) return false
        if (list[index] == item) return false

        scope.launch(Dispatchers.IO) {
            creatorDb.update(item.username, item)
                .onSuccess {
                    Timber.i("R_Saved_Creator updateIfSaved() id:${item.username}")
                    withContext(Dispatchers.Main) {
                        val idx = list.indexOfFirst { it.username == item.username }
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

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            creatorDb.refresh()
        }
    }

}
