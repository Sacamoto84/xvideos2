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
import androidx.compose.runtime.Stable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Элемент состояния выбранного автора в фильтре ленты подписок.
 *
 * @property name Никнейм автора.
 * @property select Флаг включения контента автора в общую ленту подписок.
 * @property urlProfile URL аватара автора для круглого бейджа в UI.
 */
@Stable
data class SelectedCreator(val name: String, var select: Boolean, val urlProfile : String?)

/**
 * Менеджер подписок на авторов RedGifs.
 *
 * Позволяет:
 * - Подписываться / отписываться от авторов с сохранением в `AppPath.r_subscriptions`;
 * - Выбирать авторов для фильтрации контента в ленте подписок ([selectedListCreator]);
 * - Агрегировать свежие работы всех выбранных авторов ([refreshSubscription]).
 *
 * @property scope Скоп для корутин.
 * @property redApi Сетевой клиент RedGifs.
 */
class R_Saved_Subscriptions(
    val scope: CoroutineScope,
    val redApi: RedApi,
) {

    private val creatorDb = FileDB(AppPath.r_subscriptions, "subscriptions", UserInfo.serializer())

    /**
     * Список авторов, на которых оформлена подписка.
     */
    val listCreators = creatorDb.list

    /**
     * Интерактивный список авторов с чекбоксами выбора для ленты подписок.
     */
    val selectedListCreator = mutableStateListOf<SelectedCreator>()


    init {
        refresh()
    }

    /**
     * Синхронизирует [selectedListCreator] со списком сохраненных авторов [listCreators]:
     * добавляет новых и удаляет отписанных.
     */
    private fun syncSelectedList() {
        val currentNames = HashSet<String>(selectedListCreator.size)
        for (sc in selectedListCreator) {
            currentNames.add(sc.name)
        }
        val creatorsSet = HashSet<String>(listCreators.size)
        // Добавляем новых, которых нет в списке
        for (creator in listCreators) {
            creatorsSet.add(creator.username)
            if (creator.username !in currentNames) {
                selectedListCreator.add(SelectedCreator(creator.username, true, creator.profileImageUrl))
            }
        }
        // Удаляем тех, кого больше нет в подписках
        selectedListCreator.removeAll { it.name !in creatorsSet }
    }

    /**
     * Оформляет подписку на автора [item] и сохраняет в БД.
     */
    fun add(item: UserInfo) {
        if (item.username.isBlank()) return
        Timber.i("R_Saved_Subscriptions add() id:$item")
        scope.launch(Dispatchers.IO) {
            creatorDb.insert(item.username, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = listCreators.indexOfFirst { it.username == item.username }
                        if (existingIndex >= 0) {
                            listCreators.removeAt(existingIndex)
                        }
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

    /**
     * Отменяет подписку на автора по никнейму [username].
     */
    fun remove(username: String) {
        if (username.isBlank()) return
        Timber.i("R_Saved_Subscriptions remove() id:$username")
        scope.launch(Dispatchers.IO) {
            creatorDb.delete(username)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = listCreators.indexOfFirst { it.username == username }
                        if (existingIndex >= 0) {
                            listCreators.removeAt(existingIndex)
                        }
                        syncSelectedList()
                    }
                    SnackBar.info("Автор удален")
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления Автора ${e.message}") }
        }
    }

    /**
     * Перечитывает список подписок с диска и синхронизирует состояние UI.
     */
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            creatorDb.refresh()
            withContext(Dispatchers.Main) {
                syncSelectedList()
            }
        }
    }

    /** Загружает последние 50 гифок автора по его никнейму. */
    private suspend fun read50LastItem(name: String): List<GifsInfo> {
        return redApi.searchCreator(userName = name, count = 50, type = MediaType.ALL).getOrThrow().gifs.sanitizeGifsInfoList()
    }

    /**
     * Загружает и объединяет последние гифки от всех авторов, у которых стоит флаг [SelectedCreator.select].
     *
     * @return Дедуплицированный объединенный список гифок.
     */
    suspend fun refreshSubscription() : List<GifsInfo> {
        val selectedNames = withContext(Dispatchers.Main) {
            syncSelectedList()
            val names = ArrayList<String>(selectedListCreator.size)
            for (creator in selectedListCreator) {
                if (creator.select) {
                    names.add(creator.name)
                }
            }
            names
        }
        if (selectedNames.isEmpty()) return emptyList()

        val res = ArrayList<GifsInfo>(selectedNames.size * 25)
        val seenIds = HashSet<String>(selectedNames.size * 25)
        for (name in selectedNames) {
            try {
                val items = read50LastItem(name)
                for (item in items) {
                    if (seenIds.add(item.id)) {
                        res.add(item)
                    }
                }
            } catch (e: CancellationException) {
                // Иначе отмена гасилась и цикл продолжал дёргать сеть по всем
                // оставшимся авторам уже на отменённой корутине.
                throw e
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        return res
    }

}
