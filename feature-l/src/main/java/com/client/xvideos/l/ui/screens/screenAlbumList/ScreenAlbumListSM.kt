package com.client.xvideos.l.ui.screens.screenAlbumList

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.FacetCollectionInfo
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.net.AlbumListImplInfoAndList
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Статус загрузки конкретной страницы в списке альбомов.
 */
enum class StatusAlbumList {
    /** Загрузка завершилась с ошибкой или простаивает. */
    BUSY,
    /** В процессе сетевой загрузки. */
    DOWNLOADING,
    /** Страница успешно загружена и закэширована. */
    DOWNLOADED
}

/**
 * Контейнер данных одной страницы каталога альбомов со статусом загрузки.
 *
 * @property albumListImplInfoAndList Загруженные данные альбомов и информация пагинации.
 * @property status Текущий статус загрузки [StatusAlbumList].
 */
data class AlbumListImplInfoAndListAndStatus(
    val albumListImplInfoAndList: AlbumListImplInfoAndList? = null,
    val status: StatusAlbumList = StatusAlbumList.BUSY
)

/**
 * [ScreenModel] экрана списка альбомов Luscious с поддержкой фильтрации и пагинации.
 *
 * Управляет постраничной загрузкой списка альбомов, агрегациями фильтров (жанры, теги, количество фото),
 * состоянием боковой шторки (drawer) фильтров и пейджером страниц.
 *
 * @property inFilter Начальный фильтр списка альбомов (или null для настроек по умолчанию).
 * @property luscious Экземпляр сетевого клиента Luscious.
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class ScreenLAlbumListSM @AssistedInject constructor(
    @Assisted val inFilter: AlbumListFilter?,
    val luscious: Luscious,
) : ScreenModel {

    /** Фабрика создания [ScreenLAlbumListSM] с передачей начального [AlbumListFilter]. */
    @AssistedFactory
    interface Factory : ScreenModelFactory {
        /** Создает [ScreenLAlbumListSM] для заданного фильтра. */
        fun create(filter: AlbumListFilter?): ScreenLAlbumListSM
    }

    private var loadJob: Job? = null

    //Глобальный фильтр
    private val _filter = MutableStateFlow(inFilter ?: AlbumListFilter())
    /** Поток текущего активного фильтра каталога альбомов. */
    val filter: StateFlow<AlbumListFilter> = _filter.asStateFlow()

    /**
     * Обновляет активный фильтр каталога.
     *
     * @param filter Новый объект фильтрации [AlbumListFilter].
     */
    fun filterUpdate(filter: AlbumListFilter) {
        _filter.value = filter
    }

    /** Поток информации о фасетной коллекции и пагинации (общее число элементов, страниц). */
    val info = MutableStateFlow<FacetCollectionInfo?>(null)

    /** Поток счетчиков доступных жанров для боковой панели фильтров. */
    var filterGenreStateCount = MutableStateFlow(emptyList<AlbumListFilterGenreCountResponse>())
    /** Поток счетчиков тегов для боковой панели фильтров. */
    var filterTaggedStateCount = MutableStateFlow(emptyList<AlbumListFilterGenreCountResponse>())
    /** Поток счетчиков диапазонов количества картинок. */
    var filterPictureCountStateCount =
        MutableStateFlow(emptyList<AlbumListFilterGenreCountResponse>())

    /** Карта загруженных страниц каталога: номер страницы -> данные и статус. */
    val bigList = mutableStateMapOf<Int, AlbumListImplInfoAndListAndStatus>()

    /** Состояние шторки (Drawer) боковой панели фильтрации. */
    val drawerState = DrawerState(DrawerValue.Closed)

    /** Сохраненный индекс страницы пейджера. */
    var savedPagerPage by mutableIntStateOf(0)

    // Одна страница до первого ответа сети, а не десять: реальное число ставит
    // экран через pageCountState, когда придёт totalPages. Заглушка «10»
    // означала, что пейджер до загрузки считает, будто страниц ровно десять, и
    // разрешает листать в пустоту.
    /** Состояние горизонтального пейджера страниц каталога. */
    val statePager = DefaultPagerState1(0, 0f) { 1 }

    //var albumList = MutableStateFlow<AlbumListImpl?>(null)


    private val _isRequest = MutableStateFlow(false)
    /** Поток флага выполнения сетевого запроса. */
    val isRequest = _isRequest.asStateFlow()

    /** Карта состояний скролла для каждой страницы альбомов: номер страницы -> [LazyGridState]. */
    val stateGrid = mutableStateMapOf<Int, LazyGridState>()

    init {
        Timber.d("ScreenLAlbumListSM init")

        screenModelScope.launch {
            try {
                val agr = withContext(Dispatchers.IO) {
                    luscious.getAlbumListAggregations(1, filter.value)
                }
                if (agr.isFailure) {
                    return@launch
                }

                val agrRes = agr.getOrThrow()
                filterGenreStateCount.value = agrRes.filterGenreStateCount
                filterTaggedStateCount.value = agrRes.filterTaggedStateCount
                filterPictureCountStateCount.value = agrRes.filterPictureCountStateCount
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error loading initial data")
            }
        }
    }

    /**
     * Выполняет первичную загрузку первой страницы альбомов и агрегаций фильтров.
     */
    fun loadInitialData() {
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            try {
                _isRequest.value = true
                bigList.clear()
                bigList[0] = AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.DOWNLOADING)

                val albumListResult = withContext(Dispatchers.IO) {
                    luscious.getAlbumList(1, filter.value)
                }
                if (albumListResult.isFailure) {
                    bigList[0] = AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.BUSY)
                    val errorMsg = albumListResult.exceptionOrNull()?.message ?: "Error loading initial data"
                    Timber.w("loadInitialData failure: $errorMsg")
                    SnackBar.error(errorMsg)
                    return@launch
                }

                val res = albumListResult.getOrThrow()
                info.value = res.info
                bigList[0] = AlbumListImplInfoAndListAndStatus(res, StatusAlbumList.DOWNLOADED)

                val agr = withContext(Dispatchers.IO) {
                    luscious.getAlbumListAggregations(1, filter.value)
                }
                if (agr.isFailure) {
                    return@launch
                }

                val agrRes = agr.getOrThrow()
                filterGenreStateCount.value = agrRes.filterGenreStateCount
                filterTaggedStateCount.value = agrRes.filterTaggedStateCount
                filterPictureCountStateCount.value = agrRes.filterPictureCountStateCount
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error loading initial data")
                SnackBar.error(e.message ?: "Error loading initial data")
                bigList[0] = AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.BUSY)
            } finally {
                _isRequest.value = false
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        loadJob?.cancel()
        loadJob = null
        Timber.d("ScreenLAlbumListSM onDispose")
    }

    /**
     * Загружает данные для конкретной страницы пейджера [page] (0-indexed).
     *
     * @param page Индекс запрашиваемой страницы.
     */
    fun loadAlbumList(page: Int) {
        if (page < 0) return
        screenModelScope.launch {
            val status = bigList[page]?.status
            if (status == StatusAlbumList.DOWNLOADED) {
                Timber.d("loadAlbumList DOWNLOADED page:$page")
                return@launch
            }
            if (status == StatusAlbumList.DOWNLOADING) {
                Timber.d("loadAlbumList DOWNLOADING page:$page")
                return@launch
            }

            try {
                _isRequest.value = true
                Timber.d("loadAlbumList page:$page")
                bigList[page] = AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.DOWNLOADING)

                val albumListResult = withContext(Dispatchers.IO) {
                    luscious.getAlbumList(page + 1, filter.value)
                }
                if (albumListResult.isFailure) {
                    bigList[page] = AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.BUSY)
                    return@launch
                }

                val res = albumListResult.getOrThrow()
                info.value = res.info
                bigList[page] = AlbumListImplInfoAndListAndStatus(res, StatusAlbumList.DOWNLOADED)
            } catch (e: CancellationException) {
                // Уход с экрана посреди подгрузки страницы отменяет screenModelScope.
                // Без этого catch отмена попадала в общий блок ниже и показывала
                // снекбар с текстом отмены корутины уже на предыдущем экране.
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error loading page $page")
                SnackBar.error(e.message ?: "Error loading page $page")
                bigList[page] = AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.BUSY)
            } finally {
                _isRequest.value = false
            }
        }
    }

}

/**
 * Hilt-модуль мультибиндинга фабрики [ScreenLAlbumListSM.Factory].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumList {

    /** Привязывает фабрику [ScreenLAlbumListSM.Factory] в карте ScreenModelFactory Voyager. */
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenLAlbumListSM.Factory::class)
    abstract fun bindHiltProfilesScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenLAlbumListSM.Factory
    ): ScreenModelFactory
}
