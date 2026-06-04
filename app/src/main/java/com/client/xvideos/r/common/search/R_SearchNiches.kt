package com.client.xvideos.r.common.search

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.contains

@Singleton
class R_SearchNiches @Inject constructor(
    dao: RSearchHistoryNichesFileStore,
    val savedRed: SavedRed,
    val redApi: RedApi,
    @ApplicationScope scope: CoroutineScope
) : ISearchTemplate(scope, dao) {

    init {
        scope.launch {
            searchText.collect { input ->

                val query = input.text
                if (query.isEmpty()) {
                    searchTextSuggestions.value = emptyList()
                    return@collect
                }

                try {
                    // 1. Запрос к API (может кинуть Exception)
                    val remoteResults = try {
                        redApi.searchNichesShort(query)
                            .map { SuggestionItem(text = it.name, count = it.gifs) }
                    } catch (e: Exception) {
                        emptyList() // Если API недоступно, работаем с пустым списком
                    }

                    // 2. Поиск в локальном кэше
                    val localResults = savedRed.nichesCache.list
                        .filter { it.name.contains(query, ignoreCase = true) }
                        .map { SuggestionItem(text = it.name, count = it.gifs) }

                    // 3. Красивое слияние без дубликатов
                    // distinctBy гарантирует уникальность по тексту, даже если count немного отличается
                    val combined = (remoteResults + localResults)
                        .distinctBy { it.text.lowercase() }
                        .sortedByDescending { it.count } // Опционально: сортировка по популярности

                    searchTextSuggestions.value = combined

                } catch (e: Exception) {
                    SnackBar.error(e.localizedMessage ?: "Unknown error")
                }
            }
        }
    }

}

