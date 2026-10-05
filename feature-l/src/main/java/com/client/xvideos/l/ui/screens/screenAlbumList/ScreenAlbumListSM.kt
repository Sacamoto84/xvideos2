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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Статус загрузки конкретной страницы в списке альбомов.
 */
enum class StatusAlbumList {
    /** Страница простаивает: загрузка ещё не начиналась. */
    BUSY,
    /** В процессе сетевой загрузки. */
    DOWNLOADING,
    /** Страница успешно загружена и закэширована. */
    DOWNLOADED,
    /**
     * Загрузка не удалась; причина в [AlbumListImplInfoAndListAndStatus.errorMessage].
     * Раньше отказ сбрасывал страницу в [BUSY], и экран показывал пустую сетку
     * без единого слова — об ошибке можно было узнать только из logcat.
     */
    ERROR,
}

/**
 * Контейнер данных одной страницы каталога альбомов со статусом загрузки.
 *
 * @property albumListImplInfoAndList Загруженные данные альбомов и информация пагинации.
 * @property status Текущий статус загрузки [StatusAlbumList].
 * @property errorMessage Текст ошибки для пользователя при [StatusAlbumList.ERROR].
 */
data class AlbumListImplInfoAndListAndStatus(
    val albumListImplInfoAndList: AlbumListImplInfoAndList? = null,
    val status: StatusAlbumList = StatusAlbumList.BUSY,
    val errorMessage: String? = null,
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

    /**
     * Загрузки текущего фильтра: страницы и счётчики. Смена фильтра отменяет их
     * все разом — раньше отменялась только предыдущая первая загрузка, и ответы
     * страниц прежнего фильтра дописывались в список уже под новым.
     */
    private val filterLoads: MutableSet<Job> = ConcurrentHashMap.newKeySet()

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
    val filterGenreStateCount = MutableStateFlow(emptyList<AlbumListFilterGenreCountResponse>())
    /** Поток счетчиков тегов для боковой панели фильтров. */
    val filterTaggedStateCount = MutableStateFlow(emptyList<AlbumListFilterGenreCountResponse>())
    /** Поток счетчиков диапазонов количества картинок. */
    val filterPictureCountStateCount =
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

    private val _requestsInFlight = MutableStateFlow(0)
    /**
     * Сколько страниц сейчас загружается. Счётчик, а не флаг: экран грузит до
     * четырёх страниц сразу, и первая завершившаяся гасила индикатор остальных.
     */
    val requestsInFlight: StateFlow<Int> = _requestsInFlight.asStateFlow()

    /** Карта состояний скролла для каждой страницы альбомов: номер страницы -> [LazyGridState]. */
    val stateGrid = mutableStateMapOf<Int, LazyGridState>()

    init {
        Timber.d("ScreenLAlbumListSM init")
        val filter = filter.value
        launchForFilter { loadAggregations(filter) }
    }

    /** Запускает загрузку, которую отменит смена фильтра. */
    private fun launchForFilter(block: suspend () -> Unit) {
        val job = screenModelScope.launch(start = CoroutineStart.LAZY) { block() }
        filterLoads += job
        job.invokeOnCompletion { filterLoads -= job }
        job.start()
    }

    /**
     * Загружает список заново под текущий фильтр: первую страницу и счётчики.
     * Всё, что относилось к прежнему фильтру, отменяется и стирается.
     */
    fun loadInitialData() {
        filterLoads.toList().forEach { it.cancel() }
        // Число страниц и счётчики прежнего фильтра к новому не относятся: если
        // новые не придут, старые показывать нельзя.
        info.value = null
        filterGenreStateCount.value = emptyList()
        filterTaggedStateCount.value = emptyList()
        filterPictureCountStateCount.value = emptyList()
        bigList.clear()

        val filter = filter.value
        launchForFilter { loadPage(page = 0, filter = filter, notifyFailure = true) }
        launchForFilter { loadAggregations(filter) }
    }

    override fun onDispose() {
        super.onDispose()
        Timber.d("ScreenLAlbumListSM onDispose")
    }

    /**
     * Загружает данные для конкретной страницы пейджера [page] (0-indexed).
     *
     * @param page Индекс запрашиваемой страницы.
     */
    fun loadAlbumList(page: Int) {
        if (page < 0) return
        val filter = filter.value
        launchForFilter { loadPage(page, filter, notifyFailure = false) }
    }

    /**
     * @param notifyFailure показать отказ снекбаром. Для первой страницы после
     * смены фильтра — да. Для подгрузки при листании — нет: экран грузит сразу
     * до четырёх соседних страниц, и одинаковых сообщений было бы четыре;
     * причину показывает сама страница вместе с кнопкой повтора.
     */
    private suspend fun loadPage(page: Int, filter: AlbumListFilter, notifyFailure: Boolean) {
        val status = bigList[page]?.status
        if (status == StatusAlbumList.DOWNLOADED || status == StatusAlbumList.DOWNLOADING) {
            Timber.d("loadAlbumList $status page:$page")
            return
        }

        _requestsInFlight.update { it + 1 }
        try {
            Timber.d("loadAlbumList page:$page")
            bigList[page] = AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.DOWNLOADING)

            val albumListResult = onIo { luscious.getAlbumList(page + 1, filter) }
            albumListResult
                .onSuccess { res ->
                    info.value = res.info
                    bigList[page] = AlbumListImplInfoAndListAndStatus(res, StatusAlbumList.DOWNLOADED)
                }
                .onFailure { error ->
                    val errorMsg = error.toLUserMessage()
                    Timber.w("loadAlbumList page:$page failure: ${error.javaClass.simpleName}")
                    bigList[page] = AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.ERROR, errorMsg)
                    if (notifyFailure) SnackBar.error(errorMsg)
                }
        } finally {
            _requestsInFlight.update { it - 1 }
        }
    }

    /**
     * Запрос на IO. Непредвиденное исключение превращается в отказ, как и
     * обычная ошибка сети: иначе оно уронило бы область экрана.
     */
    private suspend fun <T> onIo(request: suspend () -> Result<T>): Result<T> = try {
        withContext(Dispatchers.IO) { request() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e("ScreenLAlbumListSM request failed: ${e.javaClass.simpleName}")
        Result.failure(e)
    }

    private suspend fun loadAggregations(filter: AlbumListFilter) {
        val aggregations = onIo { luscious.getAlbumListAggregations(1, filter) }.getOrElse { error ->
            Timber.w("loadAggregations failure: ${error.javaClass.simpleName}")
            return
        }
        filterGenreStateCount.value = aggregations.filterGenreStateCount
        filterTaggedStateCount.value = aggregations.filterTaggedStateCount
        filterPictureCountStateCount.value = aggregations.filterPictureCountStateCount
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
