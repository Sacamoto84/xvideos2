package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.net.AlbumTopHitsImpl
import com.client.xvideos.l.net.Luscious
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
class ScreenLAlbumTopHitsSM @Inject constructor(
    val luscious: Luscious
) : ScreenModel {

    val state = LazyListState()

    private val _albumTopHits = MutableStateFlow<AlbumTopHitsImpl?>(null)
    val albumTopHits: StateFlow<AlbumTopHitsImpl?> = _albumTopHits.asStateFlow()

    init {
        Timber.d("iii ScreenLAlbumTopHitsSM init")
        screenModelScope.launch {
            _albumTopHits.value = luscious.getAlbumTopHits()
        }
    }

    override fun onDispose() {
        super.onDispose()
        Timber.d("iii ScreenLAlbumTopHitsSM onDispose")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumTopHits {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLAlbumTopHitsSM::class)
    abstract fun bindHiltProfilesScreenModelFactory(hiltListScreenModel: ScreenLAlbumTopHitsSM): ScreenModel
}
