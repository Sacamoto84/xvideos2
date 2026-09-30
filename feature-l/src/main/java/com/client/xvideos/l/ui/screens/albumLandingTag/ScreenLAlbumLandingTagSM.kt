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
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.net.Luscious
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

    init {
        Timber.d("ScreenLAlbumLandingTagSM init")
        screenModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    luscious.getLandingPageAlbumTag(tag)
                }
                _albumTopHits.value = res.getOrNull()
                if (res.isFailure) {
                    Timber.w(res.exceptionOrNull(), "ScreenLAlbumLandingTagSM: failed to load tag $tag")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "ScreenLAlbumLandingTagSM: exception loading tag $tag")
            }
        }

        depthState.depth = 100
    }

    override fun onDispose() {
        super.onDispose()
        Timber.d("ScreenLAlbumLandingTagSM onDispose")
    }

    fun createFilter(item: Landing_page_albumSection): AlbumListFilter {
        val title = item.title

        val albumType = when (title) {
            "Hentai Manga" -> AlbumType.Manga
            "Hentai Pictures" -> AlbumType.Pictures
            "Porn Pictures" -> AlbumType.Pictures
            else -> AlbumType.Pictures
        }

        val contentId = when (title) {
            "Hentai Manga" -> ContentId.All
            "Hentai Pictures" -> ContentId.Hentai
            "Porn Pictures" -> ContentId.RealPeople
            else -> ContentId.All
        }

        return AlbumListFilter(
            display = "date_trending",
            album_type = albumType,
            content_id = contentId,
            tagPlus = listOf(tag)
        )
    }
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
