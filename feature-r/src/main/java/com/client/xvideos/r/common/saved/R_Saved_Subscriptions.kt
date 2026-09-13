package com.client.xvideos.r.common.saved

import androidx.compose.runtime.mutableStateListOf
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


data class SelectedCreator(val name: String, var select: Boolean, val urlProfile : String?)

class R_Saved_Subscriptions(
    val scope: CoroutineScope,
    val redApi: RedApi,
) {

    private val creatorDb = FileDB(AppPath.r_subscriptions, "subscriptions", UserInfo.serializer())

    /**
     * Список авторов на которых подписаны
     */
    var listCreators = creatorDb.list

    val selectedListCreator = mutableStateListOf<SelectedCreator>()


    init {
        refresh()
    }

    private fun syncSelectedList() {
        val currentNames = selectedListCreator.map { it.name }.toSet()
        // Добавляем новых, которых нет в списке
        listCreators.map{it.username}.filter { it !in currentNames }.forEach {
            selectedListCreator.add(SelectedCreator(it, true, listCreators.firstOrNull{ itt -> itt.username  ==  it}?.profileImageUrl ))
        }
        // Удаляем тех, кого больше нет в подписках
        val creatorsSet = listCreators.map{it.username}.toSet()
        selectedListCreator.removeAll { it.name !in creatorsSet }
    }

    fun add(item: UserInfo) {
        Timber.i("R_Saved_Subscriptions add() id:$item")
        scope.launch(Dispatchers.IO) {
            creatorDb.insert(item.username, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        listCreators.removeAll { it.username == item.username }
                        listCreators.add(item)
                        syncSelectedList()
                    }
                    SnackBar.success("Автор добавлен")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления Автора ${e.message}")
                }
        }
    }

    fun remove(username: String) {
        Timber.i("R_Saved_Subscriptions remove() id:$username")
        scope.launch(Dispatchers.IO) {
            creatorDb.delete(username)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        listCreators.removeAll { it.username == username }
                        syncSelectedList()
                    }
                    SnackBar.info("Автор удален")
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления Автора ${e.message}") }
        }
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            creatorDb.refresh()
            withContext(Dispatchers.Main) {
                syncSelectedList()
            }
        }
    }


    private suspend fun read50LastItem(name: String): List<GifsInfo> {
        return redApi.searchCreator(userName = name, count = 50, type = MediaType.ALL).getOrThrow().gifs.sanitizeGifsInfoList()
    }


    suspend fun refreshSubscription() : List<GifsInfo>{
        val res  = mutableListOf<GifsInfo>()

        withContext(Dispatchers.Main) {
            syncSelectedList()
        }

        selectedListCreator.filter { it.select }.forEach {
            try {
                res.addAll(read50LastItem(it.name))
            }
            catch (e: CancellationException){
                // Иначе отмена гасилась и цикл продолжал дёргать сеть по всем
                // оставшимся авторам уже на отменённой корутине.
                throw e
            }
            catch (e: Exception){
                Timber.e(e)
            }
        }
        return res
    }


}
