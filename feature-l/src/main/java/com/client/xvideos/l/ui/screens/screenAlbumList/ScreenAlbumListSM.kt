package com.client.xvideos.l.ui.screens.screenAlbumList

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class StatusAlbumList {
    BUSY,
    DOWNLOADING,
    DOWNLOADED
}

data class AlbumListImplInfoAndListAndStatus(
    val albumListImplInfoAndList: AlbumListImplInfoAndList? = null,
    val status: StatusAlbumList = StatusAlbumList.BUSY
)

@OptIn(ExperimentalFoundationApi::class)
class ScreenLAlbumListSM @AssistedInject constructor(
    @Assisted val inFilter: AlbumListFilter?,
    val luscious: Luscious,
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(filter: AlbumListFilter?): ScreenLAlbumListSM
    }

    //Глобальный фильтр
    private val _filter = MutableStateFlow(inFilter ?: AlbumListFilter())
    val filter: StateFlow<AlbumListFilter> = _filter.asStateFlow()

    fun filterUpdate(filter: AlbumListFilter) {
        _filter.value = filter
    }

    val info = MutableStateFlow<FacetCollectionInfo?>(null)

    var filterGenreStateCount = MutableStateFlow(emptyList<AlbumListFilterGenreCountResponse>())
    var filterTaggedStateCount = MutableStateFlow(emptyList<AlbumListFilterGenreCountResponse>())
    var filterPictureCountStateCount =
        MutableStateFlow(emptyList<AlbumListFilterGenreCountResponse>())

    val bigList = mutableStateMapOf<Int, AlbumListImplInfoAndListAndStatus>()

    val drawerState = DrawerState(DrawerValue.Closed)

    var savedPagerPage by mutableIntStateOf(0)

    val statePager = DefaultPagerState1(0, 0f) { 10 }

    //var albumList = MutableStateFlow<AlbumListImpl?>(null)


    private val _isRequest = MutableStateFlow(false)
    val isRequest = _isRequest.asStateFlow()

    val stateGrid = mutableStateMapOf<Int, LazyGridState>()

    init {
        Timber.i("iii ScreenLAlbumListSM init")


        screenModelScope.launch {
            try {
                //_isRefreshing.value = true

                val agr = luscious.getAlbumListAggregations(1, filter.value)
                if (agr.isFailure) {
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    val agrRes = agr.getOrThrow()
                    filterGenreStateCount.value = agrRes.filterGenreStateCount
                    filterTaggedStateCount.value = agrRes.filterTaggedStateCount
                    filterPictureCountStateCount.value = agrRes.filterPictureCountStateCount
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error loading initial data")
            } finally {
                // _isRefreshing.value = false
            }
        }
        //loadInitialData()
        //val a = filter.value!!.toString().toMD5()
    }

    fun loadInitialData() {
        screenModelScope.launch {
            //_isRefreshing.value = true
                try {
                    val a = luscious.getAlbumList(1, filter.value)
                    if (a.isFailure) {
                        return@launch
                    }

                withContext(Dispatchers.Main) {
                    val res = a.getOrThrow()
                    info.value = res.info
                    bigList.clear()
                    delay(100)
                    bigList.put(
                        0,
                        AlbumListImplInfoAndListAndStatus(res, StatusAlbumList.DOWNLOADED)
                    )
                }

                val agr = luscious.getAlbumListAggregations(1, filter.value)
                if (agr.isFailure) {
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    val agrRes = agr.getOrThrow()
                    filterGenreStateCount.value = agrRes.filterGenreStateCount
                    filterTaggedStateCount.value = agrRes.filterTaggedStateCount
                    filterPictureCountStateCount.value = agrRes.filterPictureCountStateCount
                }

                //albumList.value?.getAlbumList(1, filter.value)
                //albumList.value?.getAlbumListAggregations(1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error loading initial data")
                SnackBar.error(e.message ?: "Error loading initial data")
            } finally {
                //_isRefreshing.value = false
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        Timber.i("iii ScreenLAlbumListSM onDispose")
    }

    fun loadAlbumList(page: Int) {

        screenModelScope.launch(Dispatchers.IO) {

            when (bigList[page]?.status) {

                StatusAlbumList.DOWNLOADED -> {
                    Timber.i("!!! loadAlbumList DOWNLOADED page:$page")
                    return@launch
                }

                StatusAlbumList.DOWNLOADING -> {
                    Timber.i("!!! loadAlbumList DOWNLOADING page:$page")
                    return@launch
                }

                else -> {}
            }

            try {
                _isRequest.value = true

                Timber.i("!!! loadAlbumList page:$page")
                bigList.put(  page, AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.DOWNLOADING) )
                val a = luscious.getAlbumList(page + 1, filter.value)
                if (a.isFailure) {
                    bigList.put(page, AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.BUSY))
                    return@launch
                }

                val res = a.getOrThrow()
                info.value = res.info
                bigList.put( page, AlbumListImplInfoAndListAndStatus(res, StatusAlbumList.DOWNLOADED) )

            } catch (e: CancellationException) {
                // Уход с экрана посреди подгрузки страницы отменяет screenModelScope.
                // Без этого catch отмена попадала в общий блок ниже и показывала
                // снекбар с текстом отмены корутины уже на предыдущем экране.
                throw e
            } catch (e: Exception) {
                Timber.e(e, "!!! eee Error loading page $page")
                SnackBar.error(e.message ?: "Error loading page $page")
                bigList.put(page, AlbumListImplInfoAndListAndStatus(null, StatusAlbumList.BUSY))
            } finally {
                _isRequest.value = false
            }
        }
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumList {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenLAlbumListSM.Factory::class)
    abstract fun bindHiltProfilesScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenLAlbumListSM.Factory
    ): ScreenModelFactory
}
