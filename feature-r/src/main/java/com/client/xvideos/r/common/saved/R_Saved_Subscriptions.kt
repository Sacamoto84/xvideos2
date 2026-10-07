package com.client.xvideos.r.common.saved

import androidx.compose.runtime.mutableStateListOf
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.Stable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
data class SelectedCreator(val name: String, val select: Boolean, val urlProfile : String?)

/**
 * Менеджер подписок на авторов R.
 *
 * Позволяет:
 * - Подписываться / отписываться от авторов с сохранением в `AppPath.r_subscriptions`;
 * - Выбирать авторов для фильтрации контента в ленте подписок ([selectedListCreator]);
 * - Агрегировать свежие работы всех выбранных авторов ([refreshSubscription]).
 *
 * @property scope Скоп для корутин.
 * @property redApi Сетевой клиент R.
 */
class R_Saved_Subscriptions(
    val scope: CoroutineScope,
    val redApi: RedApi,
    private val loadCreatorGifs: suspend (name: String) -> List<GifsInfo> = { name -> redApi.lastGifsOf(name) },
    private val notifyPartialFailure: (String) -> Unit = SnackBar::warning,
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
        Timber.i("R_Saved_Subscriptions add() id:${item.username}")
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

    /** Проверяет, пуст ли список подписок на авторов. */
    val isEmpty: Boolean get() = listCreators.isEmpty()

    /** Проверяет наличие оформленных подписок на авторов. */
    val isNotEmpty: Boolean get() = listCreators.isNotEmpty()

    /** Количество подписок на авторов. */
    val count: Int get() = listCreators.size

    /** Проверяет наличие подписки на автора по [username]. */
    fun contains(username: String): Boolean =
        username.isNotBlank() && listCreators.any { it.username == username }

    /** Проверяет наличие подписки на автора [item]. */
    fun contains(item: UserInfo?): Boolean = item != null && contains(item.username)

    /** Поиск автора в подписках по [username]. */
    fun findByUsernameOrNull(username: String?): UserInfo? =
        if (username.isNullOrBlank()) null else listCreators.firstOrNull { it.username == username }


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
        return loadSubscriptionFeed(selectedNames)
    }

    /**
     * Загружает ролики авторов [selectedNames] и объединяет их без повторов.
     *
     * Авторы запрашиваются по [SUBSCRIPTIONS_PARALLEL_REQUESTS] сразу: раньше —
     * строго по одному, и лента появлялась после последнего ответа.
     *
     * @throws Exception если не загрузился ни один автор. Раньше сбой каждого
     * автора проглатывался, и без сети лента выглядела как «у авторов нет
     * роликов» — без сообщения и кнопки повтора.
     */
    internal suspend fun loadSubscriptionFeed(selectedNames: List<String>): List<GifsInfo> {
        if (selectedNames.isEmpty()) return emptyList()

        val permits = Semaphore(SUBSCRIPTIONS_PARALLEL_REQUESTS)
        // runCatchingCancellable: отмена обязана оборвать загрузку, а не
        // записаться в сбои и продолжить дёргать сеть по остальным авторам.
        val loaded = coroutineScope {
            selectedNames.map { name ->
                async { permits.withPermit { runCatchingCancellable { loadCreatorGifs(name) } } }
            }.awaitAll()
        }

        val failures = loaded.mapNotNull { it.exceptionOrNull() }
        failures.forEach { Timber.e("R_Saved_Subscriptions: автор не загрузился: ${it.javaClass.simpleName}") }
        if (failures.size == loaded.size) throw failures.first()
        if (failures.isNotEmpty()) {
            notifyPartialFailure("Лента подписок: не загрузились ролики ${failures.size} из ${loaded.size} авторов")
        }

        val seenIds = HashSet<String>(selectedNames.size * 25)
        return loaded.flatMap { it.getOrNull().orEmpty() }.filter { seenIds.add(it.id) }
    }

}

/** Сколько авторов лента подписок запрашивает одновременно. */
internal const val SUBSCRIPTIONS_PARALLEL_REQUESTS = 4

/** Последние 50 роликов автора [name]. */
private suspend fun RedApi.lastGifsOf(name: String): List<GifsInfo> =
    searchCreator(userName = name, count = 50, type = MediaType.ALL).getOrThrow().gifs.sanitizeGifsInfoList()
