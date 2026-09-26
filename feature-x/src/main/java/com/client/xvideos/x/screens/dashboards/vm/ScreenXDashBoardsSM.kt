package com.client.xvideos.x.screens.dashboards.vm

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ScreenModel главного экрана дашбордов (лент и категорий) раздела X.
 *
 * Управляет состоянием вкладок, пагинатором, открытием плеера и действиями над избранным/загрузками.
 *
 * @property saved Фасад локальных данных раздела X ([SavedX]).
 */
@Stable
class ScreenXDashBoardsScreenModel @Inject constructor(
    val saved : SavedX
) : ScreenModel {

    /** Состояние горизонтального пейджера страниц выдачи. */
    val pagerState: PagerState = PagerState(0) { 20000 }

    /** Текущий главный таб нижней панели: 0 — Dashboards, 1 — Savable (сохранённое). */
    var mainTab by mutableIntStateOf(0)

    /** Текущий под-таб раздела Savable: 0 — Favorites (пока единственный). */
    var savedTab by mutableIntStateOf(0)

    /**
     * Открывает экран видеоплеера по карточке ролика.
     *
     * @param item Карточка ролика [ItemsX].
     * @param navigator Навигатор Voyager.
     */
    fun openVideoPlayer(item: ItemsX, navigator: Navigator) {
        navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item))
    }

    /**
     * Открывает экран видеоплеера по прямому URL.
     *
     * @param url Ссылка на видеоролик.
     * @param navigator Навигатор Voyager.
     */
    fun openVideoPlayer(url: String, navigator: Navigator) {
        navigator.push(ScreenX_VideoPlayer(url))
    }

    /** Скачать (сохранить) видео в раздел «Сохранённое». */
    fun download(item: ItemsX) = saved.downloads.download(item)

    /** Сохранить видео целиком в общую галерею (Movies/xvideos_download). */
    fun saveToGallery(item: ItemsX) = saved.downloads.saveToGallery(item)

    /** Добавить ролик в избранное. */
    fun addFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.add(item) }

    /** Удалить ролик из избранного. */
    fun removeFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.remove(item) }

    /** Быстрая O(1) проверка нахождения в избранном. */
    fun isFavorite(id: Long): Boolean = saved.favorites.contains(id) // O(1) по множеству id

}

/**
 * Hilt-модуль привязки [ScreenXDashBoardsScreenModel] в многопользовательскую карту ScreenModel.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleDashBoards {

    @Binds
    @IntoMap
    @ScreenModelKey(ScreenXDashBoardsScreenModel::class)
    abstract fun bindScreenDashBoardsScreenModel(hiltListScreenModel: ScreenXDashBoardsScreenModel): ScreenModel

}
