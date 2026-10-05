package com.client.xvideos.l.ui.screens.albumLandingTag

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.navigation.NavigationDepthState
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.toLUserMessage
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Stable
class ScreenLAlbumLandingTagSM @AssistedInject constructor(
    @Assisted val tag: String,
    val luscious: Luscious,
    depthState: NavigationDepthState
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(tag: String): ScreenLAlbumLandingTagSM
    }

    val state = LazyListState()

    private val _albumTopHits = MutableStateFlow<Landing_page_albumType?>(null)
    val albumTopHits: StateFlow<Landing_page_albumType?> = _albumTopHits.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    /** Текст сбоя загрузки страницы тега; `null`, пока она идёт или удалась. */
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private var loadJob: Job? = null

    init {
        Timber.d("ScreenLAlbumLandingTagSM init")
        load()
        depthState.depth = 100
    }

    /**
     * Повторяет загрузку страницы тега. Раньше она шла один раз при создании:
     * после сбоя экран оставался пустым, пока его не откроют заново.
     */
    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        _loadError.value = null
        loadJob = screenModelScope.launch {
            try {
                withContext(Dispatchers.IO) { luscious.getLandingPageAlbumTag(tag) }
                    .onSuccess { _albumTopHits.value = it }
                    .onFailure { error ->
                        Timber.w(error, "ScreenLAlbumLandingTagSM: failed to load tag $tag")
                        _loadError.value = error.toLUserMessage()
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "ScreenLAlbumLandingTagSM: exception loading tag $tag")
                _loadError.value = e.toLUserMessage()
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        Timber.d("ScreenLAlbumLandingTagSM onDispose")
    }

    fun createFilter(item: Landing_page_albumSection): AlbumListFilter = createAlbumTagFilter(item, tag)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumLandingTag {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenLAlbumLandingTagSM.Factory::class)
    abstract fun bindHiltLandingTagScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenLAlbumLandingTagSM.Factory
    ): ScreenModelFactory
}
