package com.client.xvideos.l.ui.screens.explorer.tab.albumSearch

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.toLUserMessage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Stable
class ScreenLAlbumSearchSM @Inject constructor(
    val luscious: Luscious
) : ScreenModel {

    val state = LazyListState()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _result = MutableStateFlow<Landing_page_albumType?>(null)
    val result: StateFlow<Landing_page_albumType?> = _result.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun clearSearchText() {
        _searchText.value = ""
    }

    fun search() {
        val query = _searchText.value.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = screenModelScope.launch {
            _isLoading.value = true
            try {
                _result.value = withContext(Dispatchers.IO) {
                    luscious.getLandingPageAlbumSearch(query).getOrElse {
                        Timber.e(it, "ScreenLAlbumSearchSM search")
                        // Без сообщения отказ сети выглядел как «ничего не найдено».
                        SnackBar.error(it.toLUserMessage())
                        null
                    }
                }
            } finally {
                if (searchJob === coroutineContext[Job]) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun createFilter(section: Landing_page_albumSection): AlbumListFilter =
        createAlbumSearchFilter(section, _searchText.value)

    override fun onDispose() {
        super.onDispose()
        searchJob?.cancel()
        _isLoading.value = false
        Timber.d("ScreenLAlbumSearchSM onDispose")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumSearch {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLAlbumSearchSM::class)
    abstract fun bindHiltSearchScreenModelFactory(hiltListScreenModel: ScreenLAlbumSearchSM): ScreenModel
}
