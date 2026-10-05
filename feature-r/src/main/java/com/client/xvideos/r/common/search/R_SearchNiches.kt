package com.client.xvideos.r.common.search

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Стейт-холдер поиска в каталоге ниш RedGifs.
 *
 * Объединяет подсказки из двух источников:
 * 1. Сетевой API поиска ниш [RedApi.searchNichesShort];
 * 2. Локальный кэш ниш [SavedRed.nichesCache], обеспечивая работу подсказок даже при отсутствии сети.
 *
 * @param dao Хранилище истории поиска ниш.
 * @param savedRed Фасад локальных данных для доступа к кэшу ниш.
 * @param redApi Сетевой клиент RedGifs.
 * @param scope Скоп уровня приложения.
 */
@Singleton
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class R_SearchNiches @Inject constructor(
    dao: RSearchHistoryNichesFileStore,
    val savedRed: SavedRed,
    val redApi: RedApi,
    @ApplicationScope scope: CoroutineScope
) : ISearchTemplate(scope, dao) {

    init {
        // Пустой запрос проходит без паузы: список подсказок должен исчезать
        // сразу, а не через треть секунды после того, как строку очистили.
        launchSuggestions(
            query = { it.text },
            pauseMs = { query -> if (query.isEmpty()) 0L else SUGGESTIONS_DEBOUNCE_MS },
            load = ::suggestionsFor,
        )
    }

    /**
     * Выполняет поиск подсказок ниш, объединяя результаты сети и локального кэша,
     * с сортировкой по убыванию популярности (числу гифок).
     */
    private suspend fun suggestionsFor(query: String): List<SuggestionItem> {
        if (query.isEmpty()) return emptyList()

        return try {
            // Сеть. Отказ — не повод остаться совсем без подсказок: ниже есть
            // локальный кеш, по нему и ищем.
            val remoteList = redApi.searchNichesShort(query)
                .onFailure { Timber.w(it, "R_SearchNiches: подсказки ниш не пришли") }
                .getOrDefault(emptyList())

            val localList = savedRed.nichesCache.list

            // distinctBy гарантирует уникальность по тексту, даже если count
            // немного отличается.
            val combinedMap = LinkedHashMap<String, SuggestionItem>(remoteList.size + localList.size)
            for (item in remoteList) {
                combinedMap.putIfAbsent(item.name.lowercase(), SuggestionItem(text = item.name, count = item.gifs))
            }
            for (item in localList) {
                if (item.name.contains(query, ignoreCase = true)) {
                    combinedMap.putIfAbsent(item.name.lowercase(), SuggestionItem(text = item.name, count = item.gifs))
                }
            }
            combinedMap.values.sortedByDescending { it.count }
        } catch (e: CancellationException) {
            // Ввод продолжился — mapLatest отменил эту ветку штатно, ошибки нет.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "R_SearchNiches suggestions error: ${e.localizedMessage}")
            emptyList()
        }
    }
}
