package com.client.xvideos.r.ui.profile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.common.network.loadGifs
import com.client.xvideos.r.common.share.useCaseShareGifs
import com.redgifs.common.video.PlayerControls
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager

enum class TypeGifs(val value: String) {
    ALL("All"),
    GIFS("GIFs"),
    IMAGES("Images"),
}

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

    val _list = MutableStateFlow<List<GifsInfo>>(emptyList())
    val list: StateFlow<List<GifsInfo>> = _list

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

    var maxCreatorGifs = 0
    var isLoading = MutableStateFlow(false)

    /** Job текущей подгрузки страницы — нужен, чтобы [clear] мог её отменить, а не ждать. */
    private var loadJob: Job? = null

    val selector: StateFlow<Int> = Settings.red_profile_selector.field

    fun setSelector(value: Int) {
        Settings.red_profile_selector.setValue(value)
    }

    val likedHost = LazyRow123Host(
        connectivityObserver = connectivityObserver,
        scope = screenModelScope,
        typePager = TypePager.PROFILE,
        extraString = profileName,
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

            try {
                val loadedCreator = redApi.readCreator(profileName).getOrNull()
                creator = loadedCreator
                loadedCreator?.let { savedRed.creators.updateIfSaved(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                creator = null
                Timber.e(e)
                SnackBar.error(e.message.toString())
            }

            block.refreshListAndBlock(_list)
        }
    }

    var play by mutableStateOf(true)
    var mute by mutableStateOf(true)
    var autoRotate by mutableStateOf(false)

    var enableAB by mutableStateOf(false)
    var timeA by mutableFloatStateOf(3f)
    var timeB by mutableFloatStateOf(6f)

    var currentPlayerControls by mutableStateOf<PlayerControls?>(null)

    var currentPlayerTime by mutableFloatStateOf(0f)
    var currentPlayerDuration by mutableIntStateOf(0)

    var currentTikTokPage by mutableIntStateOf(0)

    val currentTikTokGifInfo: GifsInfo?
        get() = list.value.getOrNull(currentTikTokPage)

    var menuCenter by mutableStateOf(false)

    var tictikStartIndex by mutableIntStateOf(0)

    fun shareGifs(context: Context, item: GifsInfo) {
        useCaseShareGifs(context, item)
    }

    suspend fun loadNextPage(userName: String, items: Int = 100, page: Int = 1) {
        Timber.d("!!! loadNextPage isLoading.value ${isLoading.value}")
        if (isLoading.value) return

        isLoading.value = true
        loadJob = currentCoroutineContext()[Job]
        try {
            val r = loadGifs(
                userName = userName,
                items = items,
                page = page,
                ord = order,
                type = if (typeGifs == TypeGifs.GIFS) MediaType.GIF else MediaType.IMAGE,
                redApi
            ).getOrThrow()
            _tags.update { it + r.tags }
            val resp = r.gifs.sanitizeGifsInfoList()
            _list.update { it + resp }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "!!! loadNextPage failed: user=$userName page=$page")
        } finally {
            loadJob = null
            isLoading.value = false
        }
    }

    /**
     * Сбрасывает выдачу профиля.
     *
     * Раньше здесь было `while (isLoading.value) { Thread.sleep(100) }` —
     * блокирующее ожидание в потоке вызывающего. Вызывается [clear] из onClick
     * (`GifTypes_Control`), то есть с main-потока, а [loadNextPage] снимает флаг
     * `isLoading` в `finally` на `screenModelScope` (`Dispatchers.Main.immediate`).
     * Заблокированный main этот `finally` выполнить не смог бы — получался не
     * фриз, а вечный дедлок.
     *
     * Теперь загрузка не ожидается, а отменяется: страница, результат которой
     * всё равно выбрасывается, не имеет смысла, а список чистится немедленно.
     */
    fun clear() {
        loadJob?.cancel()
        loadJob = null
        _list.update { emptyList() }
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
