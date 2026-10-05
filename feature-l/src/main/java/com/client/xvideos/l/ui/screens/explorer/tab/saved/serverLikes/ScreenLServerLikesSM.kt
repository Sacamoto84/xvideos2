package com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.selectionKey
import com.client.xvideos.l.ui.screens.explorer.tab.saved.LServerPagedList
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * [ScreenModel] экрана серверных лайков картинок пользователя Luscious.
 *
 * Управляет постраничной подгрузкой лайкнутых картинок через GraphQL API Luscious,
 * обновлением состояния, удалением лайков и интеграцией с [LazyRowPictureDetailsHost].
 *
 * @param repository Репозиторий доступа к серверным подпискам и избранному.
 */
@Stable
class ScreenLServerLikesSM @Inject constructor(
    private val repository: LusciousServerFavoritesRepository
) : ScreenModel {

    /** Хост состояния сетки и выбора картинок. */
    val host = LazyRowPictureDetailsHost("l_server_likes")

    private val list = LServerPagedList(
        scope = screenModelScope,
        loadPage = repository::getServerLikedPictures,
        onReplaced = { host.replaceFilteredPictures(it) },
    )

    /** Поток списка картинок, понравившихся пользователю на сервере. */
    val pictures = list.items

    /** Поток флага фоновой загрузки (первой или следующей страницы). */
    val isLoading = list.isLoading

    /** Поток флага обновления списка с первой страницы (pull-to-refresh). */
    val isRefreshing = list.isRefreshing

    /** Поток текста ошибки загрузки либо null при успешной работе. */
    val errorMessage = list.errorMessage

    /** Подгрузка следующей страницы не удалась — см. [LServerPagedList.nextPageFailed]. */
    val nextPageFailed = list.nextPageFailed

    /** Флаг наличия доступных следующих страниц для пагинации. */
    val hasMore: Boolean get() = list.hasMore

    init {
        host.onItemRemoved = { removedPic ->
            val targetKey = removedPic.selectionKey()
            list.removeIf {
                (!it.id.isNullOrBlank() && it.id == removedPic.id) ||
                    it.selectionKey() == targetKey
            }
        }
        loadInitial()
    }

    /**
     * Удаляет картинку из локального списка хоста после снятия лайка на сервере.
     *
     * @param pic Картинка, лайк с которой был снят.
     */
    fun unlikePicture(pic: PicsDetails) {
        host.removePicture(pic)
    }

    /** Загружает начальную первую страницу серверных лайков. */
    fun loadInitial() = list.loadInitial()

    /** Загружает следующую страницу серверных лайков для бесконечного скролла. */
    fun loadNextPage() = list.loadNextPage()

    /** Пользователь ушёл от конца списка — см. [LServerPagedList.onListEndLeft]. */
    fun onListEndLeft() = list.onListEndLeft()

    /** Принудительно перезагружает список лайков с первой страницы. */
    fun refresh() = list.refresh()
}

/**
 * Hilt-модуль мультибиндинга для [ScreenLServerLikesSM].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLServerLikes {

    /** Регистрирует [ScreenLServerLikesSM] в карте ScreenModel Voyager. */
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLServerLikesSM::class)
    abstract fun bindScreenLServerLikesSM(sm: ScreenLServerLikesSM): ScreenModel
}
