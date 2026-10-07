package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import com.client.xvideos.l.repository.toLUserMessage
import com.client.xvideos.l.ui.screens.explorer.tab.saved.LServerPagedList
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * [ScreenModel] экрана подписанных (избранных на сервере) альбомов пользователя L.
 *
 * Осуществляет пагинацию серверного списка подписок, отслеживает состояние загрузки
 * и предоставляет возможность удаления альбома из подписок.
 *
 * @param repository Репозиторий доступа к серверным подпискам и избранному.
 */
@Stable
class ScreenLSubscribedAlbumsSM @Inject constructor(
    private val repository: LusciousServerFavoritesRepository
) : ScreenModel {

    /** Состояние прокрутки сетки подписанных альбомов. */
    val state = LazyGridState()

    private val list = LServerPagedList(
        scope = screenModelScope,
        loadPage = repository::getSubscribedAlbums,
        // Сетка ключует элементы тем же id: повтор в списке уронил бы её.
        keyOf = { it.id },
    )

    /** Поток списка подписанных альбомов пользователя. */
    val albums = list.items

    /** Поток индикатора первичной загрузки или пагинации. */
    val isLoading = list.isLoading

    /** Поток индикатора обновления списка (pull-to-refresh). */
    val isRefreshing = list.isRefreshing

    /** Поток текста ошибки загрузки. */
    val errorMessage = list.errorMessage

    /** Подгрузка следующей страницы не удалась — см. [LServerPagedList.nextPageFailed]. */
    val nextPageFailed = list.nextPageFailed

    /** Флаг наличия последующих страниц для загрузки. */
    val hasMore: Boolean get() = list.hasMore

    init {
        loadInitial()
    }

    /** Загружает начальную первую страницу подписанных альбомов. */
    fun loadInitial() = list.loadInitial()

    /** Загружает следующую страницу альбомов для бесконечной ленты. */
    fun loadNextPage() = list.loadNextPage()

    /** Пользователь ушёл от конца списка — см. [LServerPagedList.onListEndLeft]. */
    fun onListEndLeft() = list.onListEndLeft()

    /** Обновляет список подписок с первой страницы. */
    fun refresh() = list.refresh()

    /**
     * Отменяет подписку на альбом [album] на сервере и удаляет его из локального списка.
     *
     * @param album Альбом, подписку на который необходимо удалить.
     */
    fun unlikeAlbum(album: AlbumDetails) {
        // Повторное нажатие до ответа ничего не шлёт: второй запрос отвечал
        // ошибкой, и за «удалён из подписок» следом шло «не удалось удалить».
        if (!unlikesInFlight.add(album.id)) return
        screenModelScope.launch {
            try {
                val result = repository.unlikeAlbum(album.id)
                result.onSuccess {
                    list.removeIf { it.id == album.id }
                    SnackBar.info("Альбом удалён из подписок")
                }.onFailure { error ->
                    Timber.e("Failed to unsubscribe album ${album.id} on server: ${error.javaClass.simpleName}")
                    SnackBar.error("Не удалось удалить альбом из подписок: ${error.toLUserMessage()}")
                }
            } finally {
                unlikesInFlight.remove(album.id)
            }
        }
    }

    /** Альбомы, отписка от которых уже в пути. */
    private val unlikesInFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()
}

/**
 * Hilt-модуль мультибиндинга для [ScreenLSubscribedAlbumsSM].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSubscribedAlbums {

    /** Регистрирует [ScreenLSubscribedAlbumsSM] в карте ScreenModel Voyager. */
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLSubscribedAlbumsSM::class)
    abstract fun bindScreenLSubscribedAlbumsSM(sm: ScreenLSubscribedAlbumsSM): ScreenModel
}
