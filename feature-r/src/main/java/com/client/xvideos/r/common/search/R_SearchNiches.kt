package com.client.xvideos.r.common.search

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class R_SearchNiches @Inject constructor(
    dao: RSearchHistoryNichesFileStore,
    val savedRed: SavedRed,
    val redApi: RedApi,
    @ApplicationScope scope: CoroutineScope
) : ISearchTemplate(scope, dao) {

    /**
     * Раньше здесь висел голый `searchText.collect { … сетевой запрос … }`:
     * запрос на каждое изменение текста, без паузы и без отмены предыдущего.
     * Сейчас цепочка ждёт [SUGGESTIONS_DEBOUNCE_MS] и отменяет незаконченный
     * запрос при новом вводе — за это отвечает `mapLatest`.
     *
     * Пустой запрос проходит без паузы: список подсказок должен исчезать сразу,
     * а не через треть секунды после того, как строку очистили.
     */
    init {
        scope.launch {
            searchText
                .map { it.text }
                .distinctUntilChanged()
                .debounce { query -> if (query.isEmpty()) 0L else SUGGESTIONS_DEBOUNCE_MS }
                .mapLatest { query -> suggestionsFor(query) }
                .collect { searchTextSuggestions.value = it }
        }
    }

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
