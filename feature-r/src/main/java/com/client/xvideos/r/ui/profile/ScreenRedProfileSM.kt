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
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.network.toRUserMessage
import com.client.xvideos.r.ui.ui.MetadataLoadState
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager

/**
 * [ScreenModel] экрана профиля автора (создателя контента) в R.
 *
 * Управляет:
 * - Загрузкой информации о профиле автора [metadata];
 * - Мультивыбором тегов автора для фильтрации его ленты ([tags], [tagsSelect]);
 * - Параметрами сортировки [orderList];
 * - Состоянием хоста сетки контента [likedHost].
 *
 * @param profileName Никнейм автора.
 */
@Stable
class ScreenRedProfileSM @AssistedInject constructor(
    @Assisted val profileName: String,
    connectivityObserver: ConnectivityObserver,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
) : ScreenModel {

    /** Фабрика Assisted Injection для профиля. */
    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(profileName: String): ScreenRedProfileSM
    }

    /** Очищенный никнейм автора. */
    val cleanProfileName = profileName.trim()

    /** Данные профиля автора. */
    internal var metadata: MetadataLoadState<UserInfo> by mutableStateOf(MetadataLoadState.Loading)
        private set

    private val _tags = MutableStateFlow<Set<String>>(emptySet())
    /** Доступные теги из публикаций автора. */
    val tags: StateFlow<Set<String>> = _tags

    private val _tagsSelect = MutableStateFlow<Set<String>>(emptySet())
    /** Набор тегов, выбранных пользователем для фильтрации. */
    val tagsSelect: StateFlow<Set<String>> = _tagsSelect.asStateFlow()

    /** Добавляет список тегов к доступным. */
    fun tagsAdd(l: List<String>) {
        _tags.update { it + l }
    }

    /** Переключает состояние выбора тега [tag] (включить/исключить). */
    fun toggleSelectTag(tag: String) {
        _tagsSelect.update {
            if (tag in it) it - tag else it + tag
        }
    }

    /** Сбрасывает все выбранные теги. */
    fun resetSelectedTags() {
        _tagsSelect.value = emptySet()
    }

    /** Доступные варианты сортировки на экране профиля. */
    val orderList = listOf(Order.LATEST, Order.OLDEST, Order.TOP, Order.TOP_WEEK, Order.TOP28, Order.TRENDING)

    /** Хост сетки видеороликов автора. */
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
        tags = tagsSelect,
    )

    init {
        loadMetadata()
    }

    internal fun retryMetadata() {
        if (metadata is MetadataLoadState.Failed) loadMetadata()
    }

    private fun loadMetadata() {
        if (cleanProfileName.isBlank()) {
            metadata = MetadataLoadState.Failed("Не указан автор RedGIFs")
            return
        }

        metadata = MetadataLoadState.Loading
        screenModelScope.launch {
            val creator = runCatchingCancellable {
                redApi.readCreator(cleanProfileName).getOrThrow().also {
                    check(it.isValid) { "RedGIFs вернул пустой профиль автора" }
                }
            }.getOrElse { error ->
                Timber.w("ScreenRedProfileSM: профиль автора не загрузился: ${error.javaClass.simpleName}")
                metadata = MetadataLoadState.Failed(error.toRUserMessage())
                return@launch
            }

            metadata = MetadataLoadState.Ready(creator)
            savedRed.creators.updateIfSaved(creator)
        }
    }
}

/** Hilt-модуль Assisted-фабрики экрана профиля. */
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
