package com.client.xvideos.r.ui.explorer.tab.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemNichesResponse
import com.client.xvideos.r.model.search.SearchItemTagsResponse
import com.client.xvideos.r.network.api.RedApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
class ScreenRedExplorerSearchSM @Inject constructor(
    val redApi: RedApi
) : ScreenModel {

    private val _searchText = MutableStateFlow<String>("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val creatorsList = mutableStateListOf<SearchItemCreatorsResponse>()
    val nichesList = mutableStateListOf<SearchItemNichesResponse>()
    val tagsList = mutableStateListOf<SearchItemTagsResponse>()

    fun updateSearchText(query: String) {
        _searchText.value = query
    }

    init {
        screenModelScope.launch {
            @OptIn(FlowPreview::class)
            _searchText
                .debounce(300)
                .collectLatest { rawText ->
                    val text = rawText.trim()
                    if (text.isBlank()) {
                        creatorsList.clear()
                        _isLoading.value = false
                        return@collectLatest
                    }

                    _isLoading.value = true
                    try {
                        runCatchingCancellable { redApi.search.searchCreatorsShort(text).getOrThrow() }
                            .onSuccess { creatorsList.replaceWith(it.items) }
                            .onFailure { Timber.w(it, "Поиск авторов не удался: %s", text) }
                    } finally {
                        _isLoading.value = false
                    }
                }
        }
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedExplorerSearch {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedExplorerSearchSM::class)
    abstract fun bindScreenRedExplorerSearchSreenModel(hiltListScreenModel: ScreenRedExplorerSearchSM): ScreenModel
}
