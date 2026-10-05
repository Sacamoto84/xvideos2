package com.client.xvideos.r.ui.explorer.tab.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.network.toRUserMessage
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
class ScreenRedExplorerSearchSM internal constructor(
    private val searchCreators: suspend (query: String) -> Result<List<SearchItemCreatorsResponse>>,
    private val notifyFailure: (String) -> Unit,
) : ScreenModel {

    @Inject
    constructor(redApi: RedApi) : this(
        searchCreators = { query -> redApi.search.searchCreatorsShort(query).map { it.items } },
        notifyFailure = SnackBar::error,
    )

    private val _searchText = MutableStateFlow<String>("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val creatorsList = mutableStateListOf<SearchItemCreatorsResponse>()

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
                        runCatchingCancellable { searchCreators(text).getOrThrow() }
                            .onSuccess { creatorsList.replaceWith(it) }
                            .onFailure { error ->
                                Timber.w(error, "Поиск авторов не удался")
                                // Иначе под новым текстом остаются авторы
                                // прежнего запроса, а о сбое знает только лог.
                                creatorsList.clear()
                                notifyFailure("Поиск авторов не удался: ${error.toRUserMessage()}")
                            }
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
