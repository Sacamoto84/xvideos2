package com.client.xvideos.x.feature.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateSetOf
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.x.model.XSubscriptionItem
import com.client.xvideos.x.xProfileSlug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Хранилище и менеджер подписок раздела X (Каналы и Актрисы/Модели).
 *
 * Каналы и модели лежат в одном каталоге [AppPath.x_subscriptions] и различаются расширением файла
 * (`channels` и `models`) — это два FileDB на одну папку, лок у них общий, по каталогу. Класс
 * предоставляет реактивные snapshot-наборы [channelSlugs] и [modelSlugs] для мгновенной O(1)-проверки [isSubscribed].
 *
 * @property scope CoroutineScope для выполнения дисковых операций.
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
                    }
                    SnackBar.info("Актриса удалена из подписок")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка отписки: ${e.message}")
                }
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
            }
        }
    }

    private fun cleanSlug(slug: String): String = xProfileSlug(slug)
}
