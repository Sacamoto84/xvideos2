package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateSetOf
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.SelectedXCreator
import com.client.xvideos.x.model.XSubscriptionItem
import com.client.xvideos.x.parcer.parserChannelVideosJson
import com.client.xvideos.x.urlStart
import com.client.xvideos.x.xProfileSlug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Хранилище и менеджер подписок раздела X (Каналы и Актрисы/Модели).
 *
 * Каналы и модели лежат в одном каталоге [AppPath.x_subscriptions] и различаются расширением файла
 * (`channels` и `models`) — это два FileDB на одну папку, лок у них общий, по каталогу. Класс
 * предоставляет реактивные snapshot-наборы [channelSlugs] и [modelSlugs] для мгновенной O(1)-проверки [isSubscribed],
 * а также интерактивные списки [selectedListChannels] и [selectedListModels] для фильтрации объединённой ленты.
 *
 * @property scope CoroutineScope для выполнения дисковых и сетевых операций.
 */
@Stable
class SavedX_Subscriptions(val scope: CoroutineScope) {

    private val channelsDb = FileDB(AppPath.x_subscriptions, "channels", XSubscriptionItem.serializer())
    private val modelsDb = FileDB(AppPath.x_subscriptions, "models", XSubscriptionItem.serializer())

    /** Наблюдаемый список подписанных каналов. */
    val channelsList = channelsDb.list

    /** Наблюдаемый список подписанных актрис/моделей. */
    val modelsList = modelsDb.list

    /** Snapshot-множество slug каналов для мгновенной O(1) проверки в Compose UI. */
    val channelSlugs = mutableStateSetOf<String>()

    /** Snapshot-множество slug моделей для мгновенной O(1) проверки в Compose UI. */
    val modelSlugs = mutableStateSetOf<String>()

    /** Интерактивный список выбранных каналов для ленты подписок. */
    val selectedListChannels = mutableStateListOf<SelectedXCreator>()

    /** Интерактивный список выбранных моделей для ленты подписок. */
    val selectedListModels = mutableStateListOf<SelectedXCreator>()

    init {
        refresh()
    }

    /** Проверяет, оформлена ли подписка на канал по [slug]. */
    fun containsChannel(slug: String): Boolean =
        slug.isNotBlank() && channelSlugs.contains(cleanSlug(slug))

    /** Проверяет, оформлена ли подписка на модель по [slug]. */
    fun containsModel(slug: String): Boolean =
        slug.isNotBlank() && modelSlugs.contains(cleanSlug(slug))

    /**
     * Универсальная проверка подписки.
     *
     * @param slug Идентификатор автора.
     * @param isModel `true`, если автор является моделью/актрисой.
     */
    fun isSubscribed(slug: String, isModel: Boolean): Boolean =
        if (isModel) containsModel(slug) else containsChannel(slug)

    /**
     * Добавляет канал в подписки.
     */
    fun addChannel(item: XSubscriptionItem) {
        val slug = item.cleanSlug
        if (slug.isBlank()) return
        scope.launch(Dispatchers.IO) {
            channelsDb.insert(slug, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val idx = channelsList.indexOfFirst { it.cleanSlug == slug }
                        if (idx >= 0) channelsList.removeAt(idx)
                        channelsList.add(item)
                        channelSlugs.add(slug)
                        syncSelectedChannels()
                    }
                    SnackBar.success("Канал добавлен в подписки")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка подписки на канал: ${e.message}")
                }
        }
    }

    /**
     * Удаляет канал из подписок.
     */
    fun removeChannel(slug: String) {
        val cleaned = cleanSlug(slug)
        if (cleaned.isBlank()) return
        scope.launch(Dispatchers.IO) {
            channelsDb.delete(cleaned)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val idx = channelsList.indexOfFirst { it.cleanSlug == cleaned }
                        if (idx >= 0) channelsList.removeAt(idx)
                        channelSlugs.remove(cleaned)
                        syncSelectedChannels()
                    }
                    SnackBar.info("Канал удалён из подписок")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка отписки от канала: ${e.message}")
                }
        }
    }

    /**
     * Добавляет актрису/модель в подписки.
     */
    fun addModel(item: XSubscriptionItem) {
        val slug = item.cleanSlug
        if (slug.isBlank()) return
        scope.launch(Dispatchers.IO) {
            modelsDb.insert(slug, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val idx = modelsList.indexOfFirst { it.cleanSlug == slug }
                        if (idx >= 0) modelsList.removeAt(idx)
                        modelsList.add(item)
                        modelSlugs.add(slug)
                        syncSelectedModels()
                    }
                    SnackBar.success("Актриса добавлена в подписки")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка подписки на актрису: ${e.message}")
                }
        }
    }

    /**
     * Удаляет актрису/модель из подписок.
     */
    fun removeModel(slug: String) {
        val cleaned = cleanSlug(slug)
        if (cleaned.isBlank()) return
        scope.launch(Dispatchers.IO) {
            modelsDb.delete(cleaned)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val idx = modelsList.indexOfFirst { it.cleanSlug == cleaned }
                        if (idx >= 0) modelsList.removeAt(idx)
                        modelSlugs.remove(cleaned)
                        syncSelectedModels()
                    }
                    SnackBar.info("Актриса удалена из подписок")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка отписки: ${e.message}")
                }
        }
    }

    /**
     * Переключает подписку на автора.
     */
    fun toggle(item: XSubscriptionItem) {
        val slug = item.cleanSlug
        if (item.isModel) {
            if (containsModel(slug)) removeModel(slug) else addModel(item)
        } else {
            if (containsChannel(slug)) removeChannel(slug) else addChannel(item)
        }
    }

    private var refreshJob: Job? = null

    /**
     * Перечитывает записи из файловой базы данных и синхронизирует списки.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            channelsDb.refresh()
            modelsDb.refresh()
            withContext(Dispatchers.Main) {
                channelSlugs.clear()
                for (ch in channelsList) {
                    channelSlugs.add(ch.cleanSlug)
                }
                modelSlugs.clear()
                for (m in modelsList) {
                    modelSlugs.add(m.cleanSlug)
                }
                syncSelectedChannels()
                syncSelectedModels()
            }
        }
    }

    /**
     * Синхронизирует [selectedListChannels] со списком [channelsList].
     */
    fun syncSelectedChannels() {
        val currentSlugs = HashSet<String>(selectedListChannels.size)
        for (sc in selectedListChannels) {
            currentSlugs.add(sc.slug)
        }
        val sourceSlugs = HashSet<String>(channelsList.size)
        for (ch in channelsList) {
            val slug = ch.cleanSlug
            sourceSlugs.add(slug)
            if (slug !in currentSlugs) {
                selectedListChannels.add(SelectedXCreator(item = ch, isSelected = true))
            }
        }
        selectedListChannels.removeAll { it.slug !in sourceSlugs }
    }

    /**
     * Синхронизирует [selectedListModels] со списком [modelsList].
     */
    fun syncSelectedModels() {
        val currentSlugs = HashSet<String>(selectedListModels.size)
        for (sm in selectedListModels) {
            currentSlugs.add(sm.slug)
        }
        val sourceSlugs = HashSet<String>(modelsList.size)
        for (m in modelsList) {
            val slug = m.cleanSlug
            sourceSlugs.add(slug)
            if (slug !in currentSlugs) {
                selectedListModels.add(SelectedXCreator(item = m, isSelected = true))
            }
        }
        selectedListModels.removeAll { it.slug !in sourceSlugs }
    }

    /**
     * Загружает и объединяет последние видеоролики от всех авторов, у которых стоит флаг [SelectedXCreator.isSelected].
     *
     * @param isModel `true` для подписок на актрис, `false` для подписок на каналы.
     * @return Чередующийся и дедуплицированный объединённый список видеороликов.
     */
    suspend fun fetchAggregatedVideos(isModel: Boolean): List<ItemsX> = withContext(Dispatchers.IO) {
        val selectedCreators = withContext(Dispatchers.Main) {
            val list = if (isModel) selectedListModels else selectedListChannels
            list.filter { it.isSelected }
        }
        if (selectedCreators.isEmpty()) return@withContext emptyList()

        val prefix = if (isModel) "models" else "channels"
        coroutineScope {
            // Не больше MAX_PARALLEL_FEED_REQUESTS запросов разом: при десятках подписок
            // залп запросов упирался в ограничение частоты сайта, и неудачные авторы
            // молча выпадали из ленты.
            val limiter = Semaphore(MAX_PARALLEL_FEED_REQUESTS)
            val deferredList = selectedCreators.map { creator ->
                async { limiter.withPermit { fetchCreatorVideos(creator.slug, prefix) } }
            }

            val results = deferredList.awaitAll()
            val seenIds = HashSet<Long>()
            val combined = ArrayList<ItemsX>()

            // Чередуем видео от разных авторов (round-robin)
            val maxVideos = results.maxOfOrNull { it.size } ?: 0
            for (i in 0 until maxVideos) {
                for (creatorVideos in results) {
                    if (i < creatorVideos.size) {
                        val video = creatorVideos[i]
                        if (seenIds.add(video.id)) {
                            combined.add(video)
                        }
                    }
                }
            }
            combined
        }
    }

    /**
     * Свежие ролики автора [slug]: сначала по [prefix], при пустом ответе — по
     * соседнему префиксу (models/channels). Сбой одного автора не роняет ленту.
     */
    private suspend fun fetchCreatorVideos(slug: String, prefix: String): List<ItemsX> = try {
        val json = readHtmlFromURLDirect("$urlStart/$prefix/$slug/videos/new/0")
        if (json.isBlank() || json.trim() == "{\"videos\":[]}") {
            val altPrefix = if (prefix == "models") "channels" else "models"
            val altJson = readHtmlFromURLDirect("$urlStart/$altPrefix/$slug/videos/new/0")
            if (altJson.isNotBlank() && altJson.trim() != "{\"videos\":[]}") {
                parserChannelVideosJson(altJson)
            } else {
                parserChannelVideosJson(json)
            }
        } else {
            parserChannelVideosJson(json)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "SavedX_Subscriptions: сбой загрузки роликов для %s", slug)
        emptyList()
    }

    private fun cleanSlug(slug: String): String = xProfileSlug(slug)
}

/** Сколько авторов ленты подписок запрашивается одновременно. */
private const val MAX_PARALLEL_FEED_REQUESTS = 4
