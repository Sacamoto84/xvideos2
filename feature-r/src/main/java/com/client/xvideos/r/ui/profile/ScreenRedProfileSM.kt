package com.client.xvideos.r.ui.profile

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.network.api.RedApi
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager

enum class TypeGifs(val value: String) {
    ALL("All"),
    GIFS("GIFs"),
    IMAGES("Images"),
}

@Stable
class ScreenRedProfileSM @AssistedInject constructor(
    @Assisted val profileName: String,
    connectivityObserver: ConnectivityObserver,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
    val searchNiches: R_SearchNiches,
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(profileName: String): ScreenRedProfileSM
    }

    val cleanProfileName = profileName.trim()

    var creator: UserInfo? by mutableStateOf(null)

    private val _tags = MutableStateFlow<Set<String>>(emptySet())
    val tags: StateFlow<Set<String>> = _tags
    val tagsSelect = MutableStateFlow<Set<String>>(emptySet())

    fun tagsAdd(l: List<String>) {
        _tags.update { it + l }
    }

    fun toggleSelectTag(tag: String) {
        tagsSelect.update {
            if (tag in it) it - tag else it + tag
        }
    }

    val orderList = listOf(Order.TOP, Order.LATEST, Order.OLDEST, Order.TOP28, Order.TRENDING)
    var order by mutableStateOf(Order.LATEST)

    val typeGifsList = listOf(TypeGifs.GIFS, TypeGifs.IMAGES)
    var typeGifs by mutableStateOf(TypeGifs.GIFS)

    var isLoading = MutableStateFlow(false)

    val selector: StateFlow<Int> = Settings.red_profile_selector.field

    fun setSelector(value: Int) {
        Settings.red_profile_selector.setValue(value)
    }

    val likedHost = LazyRow123Host(
        connectivityObserver = connectivityObserver,
        scope = screenModelScope,
        typePager = TypePager.PROFILE,
        extraString = cleanProfileName,
        visibleProfileInfo = false,
        block = block,
        redApi = redApi,
        savedRed = savedRed,
        downloadRed = downloadRed,
        search = search,
        searchNiches = searchNiches,
        tags = tagsSelect,
    )

    init {
        screenModelScope.launch {
            clear()
            setSelector(2)

            if (cleanProfileName.isNotBlank()) {
                isLoading.value = true
                try {
                    val loadedCreator = redApi.readCreator(cleanProfileName).getOrNull()
                    creator = loadedCreator
                    loadedCreator?.let { savedRed.creators.updateIfSaved(it) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    creator = null
                    Timber.e(e)
                    SnackBar.error(e.message.toString())
                } finally {
                    isLoading.value = false
                }
            } else {
                Timber.w("ScreenRedProfileSM init: пустое имя профиля")
            }
        }
    }

    fun clear() {
        isLoading.value = false
        _tags.update { emptySet() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedProfile {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenRedProfileSM.Factory::class)
    abstract fun bindHiltProfilesScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenRedProfileSM.Factory
    ): ScreenModelFactory

}
