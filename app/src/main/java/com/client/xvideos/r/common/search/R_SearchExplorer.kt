package com.client.xvideos.r.common.search

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class R_SearchExplorer @Inject constructor(
    @ApplicationScope scope: CoroutineScope,
    dao: RSearchHistoryExplorerFileStore,
    redApiIn: Provider<RedApi>,
) : ISearchTemplate(scope, dao) {

    val redApi = redApiIn.get()

    init {
        scope.launch {
            searchText.collect {
                try {
                    val request = if (it.text == "") " " else it.text

                    searchTextSuggestions.value = redApi.getTagSuggestions(request)
                        .map { list -> list.map { s -> SuggestionItem(text = s.text, count = s.gifs) } }
                        .getOrDefault(emptyList())

                } catch (e: Exception) {
                    Timber.e("!!! SearchRed searchText.collect ${e.localizedMessage}")
                }
            }
        }
    }

}


